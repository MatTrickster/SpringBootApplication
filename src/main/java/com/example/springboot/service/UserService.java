package com.example.springboot.service;

import com.example.springboot.config.AppProperties;
import com.example.springboot.dto.UserRequest;
import com.example.springboot.entity.Role;
import com.example.springboot.entity.User;
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
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    /**
     * Walmart-style task: the "default user" differs per environment.
     * The username comes from app.default-user, which each profile can override.
     */
    @Transactional(readOnly = true)
    public User findDefaultUser() {
        String username = appProperties.defaultUser();
        if (username == null || username.isBlank()) {
            throw new IllegalStateException("app.default-user is not configured for this environment");
        }
        log.debug("Resolving default user '{}'", username);
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    }

    /**
     * Pagination with an explicit PageRequest. Page numbers are 0-based.
     * The page size is clamped to app.max-page-size so a client can't ask for size=1000000.
     */
    @Transactional(readOnly = true)
    public Page<User> findAll(Role role, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), appProperties.maxPageSize());
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize, Sort.by("id"));
        return role == null ? userRepository.findAll(pageable) : userRepository.findByRole(role, pageable);
    }

    /**
     * Duplicate checks are business validation: they need the DB, so they live in the
     * service, not in a Bean Validation annotation. They give a clear 409 message, but two
     * concurrent requests can both pass them. The unique constraints on the entity are the
     * real guarantee; GlobalExceptionHandler maps that DataIntegrityViolationException to 409 too.
     */
    @Transactional
    public User create(UserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("User", "username", request.username());
        }
        if (userRepository.existsByEmailIgnoreCase(request.normalizedEmail())) {
            throw new DuplicateResourceException("User", "email", request.normalizedEmail());
        }
        User saved = userRepository.save(request.toEntity());
        log.info("Created user id={} username={}", saved.getId(), saved.getUsername());
        return saved;
    }
}
