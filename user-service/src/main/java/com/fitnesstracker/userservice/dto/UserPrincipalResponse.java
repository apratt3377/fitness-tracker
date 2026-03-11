package com.fitnesstracker.userservice.dto;

import java.util.UUID;

import com.fitnesstracker.userservice.domain.UserEntity.Role;

public record UserPrincipalResponse(
    UUID id,
    String username,
    Role roles
) {};
