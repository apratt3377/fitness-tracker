package com.fitnesstracker.userservice.dto;

import java.util.List;
import java.util.UUID;

// roles is a List to support future multi-role extension without breaking the Auth Service contract.
public record UserPrincipalResponse(
    UUID id,
    String username,
    List<String> roles
) {}
