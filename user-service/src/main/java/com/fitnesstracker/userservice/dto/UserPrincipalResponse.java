package com.fitnesstracker.userservice.dto;

import java.util.UUID;

public record UserPrincipalResponse(
    UUID id,
    String username,
    String role
) {}
