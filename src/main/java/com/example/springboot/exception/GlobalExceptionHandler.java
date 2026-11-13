package com.example.springboot.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Central error handling. Extending ResponseEntityExceptionHandler gives
 * RFC 9457 ProblemDetail bodies for Spring MVC's built-in exceptions
 * (405 method not allowed, 415 unsupported media type, malformed JSON, type mismatch, ...).
 *
 * Status code map:
 *   400 invalid body / invalid query params     404 resource missing
 *   409 duplicate (service check or DB constraint)
 *   503 local overload (retry later)             504 work timed out
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /** Seconds a client should wait before retrying after a 503. */
    private static final String RETRY_AFTER_SECONDS = "1";

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Safety net for a DB unique-constraint violation that slipped past the service-level
     * duplicate check (e.g. two concurrent POSTs). The SQL message is not returned to the client.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Request conflicts with existing data");
    }

    /** Our own capacity is exhausted: tell the client it is temporary and when to retry. */
    @ExceptionHandler(ServiceOverloadedException.class)
    public ResponseEntity<ProblemDetail> handleOverloaded(ServiceOverloadedException ex) {
        log.warn("Rejecting request: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, RETRY_AFTER_SECONDS)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage()));
    }

    /**
     * 504: the work depends on the database, and it did not answer in time.
     * (Some teams prefer 503 for every timeout; the key point is "not 500".)
     */
    @ExceptionHandler(OperationTimeoutException.class)
    public ProblemDetail handleTimeout(OperationTimeoutException ex) {
        log.warn("Timed out: {}", ex.getMessage());
        return ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage());
    }

    /** @Valid @RequestBody failures: adds a field -> message map. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> errors.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return validationProblem(status, errors);
    }

    /**
     * Constraint annotations directly on @RequestParam/@PathVariable (e.g. @Min(0) int page).
     * Spring 6.1+ validates them without @Validated on the controller and throws this exception.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getParameterValidationResults().forEach(result -> result.getResolvableErrors().forEach(error ->
                errors.putIfAbsent(result.getMethodParameter().getParameterName(), error.getDefaultMessage())));
        return validationProblem(status, errors);
    }

    private static ResponseEntity<Object> validationProblem(HttpStatusCode status, Map<String, String> errors) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, "Validation failed");
        body.setProperty("errors", errors);
        return ResponseEntity.status(status).body(body);
    }
}
