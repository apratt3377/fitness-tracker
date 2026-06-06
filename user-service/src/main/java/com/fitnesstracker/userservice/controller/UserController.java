package com.fitnesstracker.userservice.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fitnesstracker.userservice.dto.UserResponse;
import com.fitnesstracker.userservice.mapper.UserMapper;
import com.fitnesstracker.userservice.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "1. User Facing", description = "Client-side endpoints (requires X-User-Id header from Gateway)")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @Operation(summary = "Get current user profile",
            description = "X-User-Id is injected by the API Gateway after JWT validation — never trust this header from external clients directly.")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(
            @Parameter(description = "UUID of authenticated user, injected by API Gateway")
            @RequestHeader("X-User-Id") UUID userId) {
        return userService.findById(userId)
                .map(userMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
