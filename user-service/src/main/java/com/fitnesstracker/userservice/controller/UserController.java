package com.fitnesstracker.userservice.controller;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.fitnesstracker.userservice.domain.UserEntity;
import com.fitnesstracker.userservice.dto.AuthRequest;
import com.fitnesstracker.userservice.dto.UserCreateRequest;
import com.fitnesstracker.userservice.dto.UserPrincipalResponse;
import com.fitnesstracker.userservice.dto.UserResponse;
import com.fitnesstracker.userservice.mapper.UserMapper;
import com.fitnesstracker.userservice.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

@RestController
public class UserController {
    private final UserService userService;
    private final UserMapper userMapper;

    public UserController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    // --- USER FACING (via Gateway) ---
    @Operation(summary= "Get current user profile", description = "Requires X-User-Id header from Gateway")
    @GetMapping("/api/users/me")
    public ResponseEntity<UserResponse> getMe(
        @Parameter(description = "UUID of user passed by Gateway")
        @RequestHeader("X-User-Id") String userId) {
        return userService.findById(UUID.fromString(userId))
                .map(userMapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // --- INTERNAL (Auth Service Integration) ---
    @Operation(summary = "Authenticate and get Principal", 
            description = "Verifies password hash and returns ID/Roles for JWT generation.")
    @PostMapping("/internal/users/authenticate")
    public ResponseEntity<UserPrincipalResponse> lookup(@RequestBody AuthRequest authRequest) {
        return userService.authenticate(authRequest.username(), authRequest.password())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @Operation(summary = "Internal: Create new user record", 
           description = "Triggered by the Auth Service during registration.")
    @PostMapping("/internal/users/create")
    public ResponseEntity<UserEntity> create(@RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    // --- ADMIN ONLY ---
    @Operation(summary = "Admin: List all users", 
           description = "Returns a list of all registered users with passwords stripped out.")
    @GetMapping("/api/admin/users")
    public ResponseEntity<Iterable<UserResponse>> listAll() {
        Iterable<UserEntity> users = userService.getAllUsers();
        
        // 2. Map them to DTOs (Records) to strip passwords
        List<UserResponse> responseList = StreamSupport.stream(users.spliterator(), false)
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
        
        // 3. Return the safe list
        return ResponseEntity.ok(responseList);
    }
    @Operation(summary = "Admin: Delete a user", 
    description = "Permanently removes a user from the accounts database.")
    @DeleteMapping("/api/admin/users/{id}")
    public ResponseEntity<Void> remove(
        @Parameter(description = "The UUID of the user to delete")
        @PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}