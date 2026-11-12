package com.example.springboot.repository;

import com.example.springboot.entity.Role;
import com.example.springboot.entity.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmailIgnoreCase(String email);

    // Adding a Pageable parameter makes Spring Data run a LIMIT/OFFSET query
    // plus a COUNT query, and return a Page with totals.
    Page<User> findByRole(Role role, Pageable pageable);
}
