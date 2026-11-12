package com.example.springboot.dto;

import com.example.springboot.entity.Role;
import com.example.springboot.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

/** Payload for POST /users. "role" must be ADMIN, QA or USER (Jackson enum names). */
public record UserRequest(
        @NotBlank @Size(min = 3, max = 30)
        @Pattern(regexp = "[a-z0-9-]+", message = "must contain only lowercase letters, digits and '-'")
        String username,
        @NotBlank @Email String email,
        @NotNull Role role) {

    /** Emails are compared case-insensitively, so store them normalized. */
    public String normalizedEmail() {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public User toEntity() {
        return User.builder().username(username).email(normalizedEmail()).role(role).build();
    }
}
