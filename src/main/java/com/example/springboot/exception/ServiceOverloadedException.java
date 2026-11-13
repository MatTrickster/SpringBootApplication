package com.example.springboot.exception;

/** Local capacity exhausted (e.g. executor queue full). Mapped to 503 + Retry-After. */
public class ServiceOverloadedException extends RuntimeException {

    public ServiceOverloadedException(String message, Throwable cause) {
        super(message, cause);
    }
}
