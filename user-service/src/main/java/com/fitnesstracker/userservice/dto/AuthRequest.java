package com.fitnesstracker.userservice.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
    @NotBlank String username,
    @NotBlank String password
) {
    @Override
    public String toString() {
        return "AuthRequest[username=" + username + ", password=***]";
    }
}
