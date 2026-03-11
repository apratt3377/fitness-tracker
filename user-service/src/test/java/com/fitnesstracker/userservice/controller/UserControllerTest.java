package com.fitnesstracker.userservice.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitnesstracker.userservice.domain.UserEntity;
import com.fitnesstracker.userservice.dto.AuthRequest;
import com.fitnesstracker.userservice.dto.UserCreateRequest;
import com.fitnesstracker.userservice.dto.UserPrincipalResponse;
import com.fitnesstracker.userservice.dto.UserResponse;
import com.fitnesstracker.userservice.mapper.UserMapper;
import com.fitnesstracker.userservice.repository.UserRepository;
import com.fitnesstracker.userservice.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

@WebMvcTest(UserController.class)
public class UserControllerTest {


    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("GET /api/users/me - Should return profile when X-User-Id header is present")
    void shouldReturnUserProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setUsername("user_a");

        UserResponse response = new UserResponse(userId, "user_a");

        when(userService.findById(userId)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        mockMvc.perform(get("/api/users/me")
                .header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user_a"));
    }

    @Test
    @DisplayName("POST /internal/users/authenticate - Should return 200 for valid credentials")
    void shouldAuthenticateUser() throws Exception {
        AuthRequest authRequest = new AuthRequest("user_a", "securePass");
        UserPrincipalResponse principal = new UserPrincipalResponse(UUID.randomUUID(), "user_a", UserEntity.Role.USER);

        when(userService.authenticate("user_a", "securePass"))
                .thenReturn(Optional.of(principal));

        mockMvc.perform(post("/internal/users/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").value("USER"));
    }

    @Test
    @DisplayName("POST /internal/users/authenticate - Should return 401 for invalid credentials")
    void shouldNotAuthenticateInvalidUser() throws Exception {
        AuthRequest authRequest = new AuthRequest("stranger", "wrongPass");

        when(userService.authenticate(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/internal/users/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(authRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /internal/users/create - Should return 201 Created")
    void shouldCreateInternalUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest("new_user", "pass", "USER");
        UserEntity savedUser = new UserEntity();
        savedUser.setUsername("new_user");

        when(userService.createUser(any(UserCreateRequest.class))).thenReturn(savedUser);

        mockMvc.perform(post("/internal/users/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_user"));
    }

    @Test
    @DisplayName("GET /api/admin/users - Should return list of all users")
    void shouldListAllUsersForAdmin() throws Exception {
        UserEntity user1 = new UserEntity();
        user1.setUsername("user_a");
        UserResponse resp1 = new UserResponse(UUID.randomUUID(), "user_a");

        // Mocking the Iterable return from service
        when(userService.getAllUsers()).thenReturn(java.util.List.of(user1));
        when(userMapper.toResponse(any(UserEntity.class))).thenReturn(resp1);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("user_a"));
    }

    @Test
    @DisplayName("DELETE /api/admin/users/{id} - Should return 204 No Content")
    void shouldDeleteUser() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/admin/users/{id}", userId))
                .andExpect(status().isNoContent());
    }

}