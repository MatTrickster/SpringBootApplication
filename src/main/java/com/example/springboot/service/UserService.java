package com.example.springboot.service;

import com.example.springboot.config.AppProperties;
import com.example.springboot.dto.UserRequest;
import com.example.springboot.dto.UserResponse;
import com.example.springboot.entity.Role;
import com.example.springboot.exception.DuplicateResourceException;
import com.example.springboot.exception.ResourceNotFoundException;
import com.example.springboot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AppProperties appProperties;

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return userRepository.findById(id)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    /**
     * Walmart-style task: the "default user" differs per environment.
     * The username comes from app.default-user, which each profile can override.
     */
    @Transactional(readOnly = true)
    public UserResponse findDefaultUser() {
        String username = appProperties.defaultUser();
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("app.default-user is not configured for this environment");
        }
        log.debug("Resolving default user '{}'", username);
        return userRepository.findByUsername(username)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /**
     * Pagination with an explicit PageRequest. Page numbers are 0-based.
     *
     * Invalid input is rejected, not silently "fixed": page=-1 meaning page 0 hides client bugs.
     * The controller already returns 400 for these (@Min on the params); these checks guard
     * other callers. Oversized pages are clamped to app.max-page-size instead of rejected,
     * which is the common API convention (the response's "size" shows the applied value).
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Role role, int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("Page number must be non-negative");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be positive");
        }
        Pageable pageable = PageRequest.of(page, Math.min(size, appProperties.maxPageSize()), Sort.by("id"));
        return (role == null ? userRepository.findAll(pageable) : userRepository.findByRole(role, pageable))
                .map(UserResponse::from);
    }

    /**
     * Duplicate checks are business validation: they need the DB, so they live in the
     * service, not in a Bean Validation annotation. They give a clear 409 message, but two
     * concurrent requests can both pass them. The unique constraints on the entity are the
     * real guarantee; GlobalExceptionHandler maps that DataIntegrityViolationException to 409 too.
     */
    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("User", "username", request.username());
        }
        if (userRepository.existsByEmailIgnoreCase(request.normalizedEmail())) {
            throw new DuplicateResourceException("User", "email", request.normalizedEmail());
        }
        UserResponse saved = UserResponse.from(userRepository.save(request.toEntity()));
        log.info("Created user id={} username={}", saved.id(), saved.username());
        return saved;
    }
}
