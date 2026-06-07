package com.fitnesstracker.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fitnesstracker.userservice.domain.Role;
import com.fitnesstracker.userservice.domain.UserEntity;
import com.fitnesstracker.userservice.dto.AdminUserResponse;
import com.fitnesstracker.userservice.dto.AuthRequest;
import com.fitnesstracker.userservice.dto.UserCreateRequest;
import com.fitnesstracker.userservice.dto.UserPrincipalResponse;
import com.fitnesstracker.userservice.dto.UserResponse;
import com.fitnesstracker.userservice.mapper.UserMapper;
import com.fitnesstracker.userservice.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalController.class)
class InternalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @Test
    @DisplayName("POST /internal/v1/identity/authenticate - returns 200 with principal for valid credentials")
    void authenticate_validCredentials_returns200() throws Exception {
        AuthRequest request = new AuthRequest("user_a", "securePass1");
        UserPrincipalResponse principal = new UserPrincipalResponse(UUID.randomUUID(), "user_a", "USER");

        when(userService.authenticate("user_a", "securePass1")).thenReturn(Optional.of(principal));

        mockMvc.perform(post("/internal/v1/identity/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user_a"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("POST /internal/v1/identity/authenticate - returns 401 for invalid credentials")
    void authenticate_invalidCredentials_returns401() throws Exception {
        AuthRequest request = new AuthRequest("stranger", "wrongPass1");
        when(userService.authenticate(any(), any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/internal/v1/identity/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /internal/v1/identity/authenticate - returns 400 when username is blank")
    void authenticate_blankUsername_returns400() throws Exception {
        AuthRequest request = new AuthRequest("", "securePass1");

        mockMvc.perform(post("/internal/v1/identity/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /internal/v1/identity/authenticate - returns 400 when password is blank")
    void authenticate_blankPassword_returns400() throws Exception {
        AuthRequest request = new AuthRequest("user_a", "");

        mockMvc.perform(post("/internal/v1/identity/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /internal/v1/identity/users - returns 201 with safe UserResponse")
    void create_validRequest_returns201() throws Exception {
        UserCreateRequest request = new UserCreateRequest("new_user", "password123", "USER");
        UserEntity savedEntity = new UserEntity();
        savedEntity.setId(UUID.randomUUID());
        savedEntity.setUsername("new_user");
        UserResponse response = new UserResponse(savedEntity.getId(), "new_user");

        when(userService.createUser(any())).thenReturn(savedEntity);
        when(userMapper.toResponse(savedEntity)).thenReturn(response);

        mockMvc.perform(post("/internal/v1/identity/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_user"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("POST /internal/v1/identity/users - returns 400 when password is too short")
    void create_shortPassword_returns400() throws Exception {
        UserCreateRequest request = new UserCreateRequest("new_user", "short", "USER");

        mockMvc.perform(post("/internal/v1/identity/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /internal/v1/identity/users - returns 400 when username contains invalid characters")
    void create_invalidUsernameChars_returns400() throws Exception {
        UserCreateRequest request = new UserCreateRequest("bad user!", "password123", "USER");

        mockMvc.perform(post("/internal/v1/identity/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /internal/v1/identity/users - returns 400 when role is invalid")
    void create_invalidRole_returns400() throws Exception {
        UserCreateRequest request = new UserCreateRequest("new_user", "password123", "SUPERUSER");

        mockMvc.perform(post("/internal/v1/identity/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /internal/v1/identity/users - returns 200 with user list")
    void listAll_returnsUserList() throws Exception {
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setUsername("user_a");
        entity.setRole(Role.USER);

        AdminUserResponse adminResponse = new AdminUserResponse(userId, "user_a", Role.USER, now);

        when(userService.getAllUsers()).thenReturn(List.of(entity));
        when(userMapper.toAdminResponse(entity)).thenReturn(adminResponse);

        mockMvc.perform(get("/internal/v1/identity/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("user_a"))
                .andExpect(jsonPath("$[0].role").value("USER"));
    }

    @Test
    @DisplayName("GET /internal/v1/identity/users - returns empty array when no users exist")
    void listAll_returnsEmptyArray_whenNoUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/internal/v1/identity/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("DELETE /internal/v1/identity/users/{id} - returns 204 for existing user")
    void delete_existingUser_returns204() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/internal/v1/identity/users/{id}", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /internal/v1/identity/users/{id} - returns 404 for non-existent user")
    void delete_nonExistentUser_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        doThrow(new EntityNotFoundException("User not found: " + userId))
                .when(userService).deleteUser(userId);

        mockMvc.perform(delete("/internal/v1/identity/users/{id}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /internal/v1/identity/users/{id} - returns 400 for invalid UUID")
    void delete_invalidUuid_returns400() throws Exception {
        mockMvc.perform(delete("/internal/v1/identity/users/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
