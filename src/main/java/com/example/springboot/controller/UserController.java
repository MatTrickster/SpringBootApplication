package com.example.springboot.controller;

import com.example.springboot.dto.PageResponse;
import com.example.springboot.dto.UserRequest;
import com.example.springboot.dto.UserResponse;
import com.example.springboot.entity.Role;
import com.example.springboot.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.Arrays;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 *   GET  /users/default                    -> default user for the active profile
 *   GET  /users/{id}                       -> 200, or 404
 *   GET  /users?role=admin&page=0&size=10  -> paged, role optional and case-insensitive
 *                                             400 if page < 0, size < 1, non-numeric or unknown role
 *   POST /users                            -> 201, 400 invalid, 409 duplicate
 *
 * "/default" and "/{id}" don't clash: Spring prefers the literal path over the template.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/default")
    public UserResponse defaultUser() {
        return userService.findDefaultUser();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    /**
     * @Min on @RequestParam works without @Validated on the class (Spring 6.1+ built-in
     * method validation) and produces HandlerMethodValidationException -> 400.
     * Don't add class-level @Validated: that switches to the older AOP validation, which
     * throws ConstraintViolationException (a 500 unless you handle it).
     */
    @GetMapping
    public PageResponse<UserResponse> findAll(@RequestParam(required = false) String role,
                                              @RequestParam(defaultValue = "0") @Min(0) int page,
                                              @RequestParam(defaultValue = "10") @Min(1) int size) {
        return PageResponse.from(userService.findAll(parseRole(role), page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    /**
     * Binding straight to a Role parameter would be case-sensitive (?role=ADMIN only).
     * Parsing here accepts "admin" and returns a clear 400 for unknown values.
     */
    private static Role parseRole(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Unknown role '" + role + "'. Allowed: " + Arrays.toString(Role.values()));
        }
    }
}
