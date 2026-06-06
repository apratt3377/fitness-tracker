package com.fitnesstracker.userservice.controller;

import com.fitnesstracker.userservice.domain.UserEntity;
import com.fitnesstracker.userservice.dto.UserResponse;
import com.fitnesstracker.userservice.mapper.UserMapper;
import com.fitnesstracker.userservice.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @Test
    @DisplayName("GET /api/v1/users/me - returns 200 with profile when user exists")
    void getMe_returnsProfile() throws Exception {
        UUID userId = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setUsername("user_a");
        UserResponse response = new UserResponse(userId, "user_a");

        when(userService.findById(userId)).thenReturn(Optional.of(entity));
        when(userMapper.toResponse(entity)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/me").header("X-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user_a"))
                .andExpect(jsonPath("$.id").value(userId.toString()));
    }

    @Test
    @DisplayName("GET /api/v1/users/me - returns 404 when user not found")
    void getMe_returnsNotFound() throws Exception {
        UUID userId = UUID.randomUUID();
        when(userService.findById(userId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/users/me").header("X-User-Id", userId.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/users/me - returns 400 when X-User-Id is not a valid UUID")
    void getMe_returnsBadRequestOnInvalidUuid() throws Exception {
        mockMvc.perform(get("/api/v1/users/me").header("X-User-Id", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/users/me - returns 400 when X-User-Id header is missing")
    void getMe_returnsBadRequestOnMissingHeader() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isBadRequest());
    }
}
