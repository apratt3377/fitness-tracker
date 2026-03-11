package com.fitnesstracker.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for internal user creation requests.
 * Used by the Auth Service to pass hashed credentials and roles.
 */
public record UserCreateRequest(
    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username must be under 50 characters")
    String username,

    @NotBlank(message = "Password is required")
    @Size(max = 255)
    String password, // This carries the pwdHash from the Auth Service

    @Size(max = 32)
    @Pattern(regexp = "USER|ADMIN", message = "Role must be either USER or ADMIN")
    String roles // Defaults to ROLE_USER if null in the controller/service
) {}