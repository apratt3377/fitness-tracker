package com.fitnesstracker.userservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fitnesstracker.userservice.dto.AdminUserResponse;
import com.fitnesstracker.userservice.dto.AuthRequest;
import com.fitnesstracker.userservice.dto.UserCreateRequest;
import com.fitnesstracker.userservice.dto.UserPrincipalResponse;
import com.fitnesstracker.userservice.dto.UserResponse;
import com.fitnesstracker.userservice.mapper.UserMapper;
import com.fitnesstracker.userservice.service.UserService;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/identity")
@Hidden
public class InternalController {

    private final UserService userService;
    private final UserMapper userMapper;

    public InternalController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @PostMapping("/authenticate")
    public ResponseEntity<UserPrincipalResponse> authenticate(@RequestBody @Valid AuthRequest request) {
        return userService.authenticate(request.username(), request.password())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> create(@RequestBody @Valid UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userMapper.toResponse(userService.createUser(request)));
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponse>> listAll() {
        List<AdminUserResponse> users = userService.getAllUsers().stream()
                .map(userMapper::toAdminResponse)
                .toList();
        return ResponseEntity.ok(users);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
