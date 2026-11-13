package com.example.springboot.dto;

import com.example.springboot.entity.Role;
import com.example.springboot.entity.User;

/**
 * Public JSON contract for a user. If User later gains a passwordHash, audit columns or a
 * lazy @OneToMany, none of it leaks into responses unless it is added here on purpose.
 */
public record UserResponse(Long id, String username, String email, Role role, boolean active) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole(), user.isActive());
    }
}
