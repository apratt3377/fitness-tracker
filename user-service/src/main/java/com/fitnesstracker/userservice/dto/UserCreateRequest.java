package com.fitnesstracker.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Auth Service sends the plaintext password; User Service is responsible for BCrypt hashing.
public record UserCreateRequest(
    @NotBlank
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "Username may only contain letters, digits, dots, hyphens, and underscores")
    String username,

    @NotBlank
    @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
    String password,

    // null → defaults to USER in service layer; any non-null value must be exactly USER or ADMIN.
    @Pattern(regexp = "USER|ADMIN", message = "Role must be USER or ADMIN")
    String role
) {}
