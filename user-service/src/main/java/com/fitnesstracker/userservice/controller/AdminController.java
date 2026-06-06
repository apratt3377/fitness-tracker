package com.fitnesstracker.userservice.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fitnesstracker.userservice.dto.AdminUserResponse;
import com.fitnesstracker.userservice.mapper.UserMapper;
import com.fitnesstracker.userservice.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "3. Admin", description = "Management and administrative tools")
public class AdminController {

    private final UserService userService;
    private final UserMapper userMapper;

    public AdminController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @Operation(summary = "List all active users", description = "Returns all non-soft-deleted user accounts with role and creation date.")
    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> listAll() {
        List<AdminUserResponse> users = userService.getAllUsers().stream()
                .map(userMapper::toAdminResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Soft-delete a user by UUID")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(
            @Parameter(description = "UUID of the user to deactivate")
            @PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
