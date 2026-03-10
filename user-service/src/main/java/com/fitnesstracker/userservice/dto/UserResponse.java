package com.fitnesstracker.userservice.dto;

import java.util.UUID;

public record UserResponse(
    UUID id,
    String username
) {};
