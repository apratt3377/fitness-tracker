package com.fitnesstracker.userservice.dto;

import com.fitnesstracker.userservice.domain.Role;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminUserResponse(
    UUID id,
    String username,
    Role role,
    OffsetDateTime createdAt
) {}
