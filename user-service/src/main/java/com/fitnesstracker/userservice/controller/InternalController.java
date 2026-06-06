package com.fitnesstracker.userservice.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fitnesstracker.userservice.dto.AuthRequest;
import com.fitnesstracker.userservice.dto.UserCreateRequest;
import com.fitnesstracker.userservice.dto.UserPrincipalResponse;
import com.fitnesstracker.userservice.dto.UserResponse;
import com.fitnesstracker.userservice.mapper.UserMapper;
import com.fitnesstracker.userservice.service.UserService;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;

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
}
