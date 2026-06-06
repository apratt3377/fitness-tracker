package com.fitnesstracker.userservice.controller;

import com.fitnesstracker.userservice.domain.Role;
import com.fitnesstracker.userservice.domain.UserEntity;
import com.fitnesstracker.userservice.dto.AdminUserResponse;
import com.fitnesstracker.userservice.mapper.UserMapper;
import com.fitnesstracker.userservice.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private UserMapper userMapper;

    @Test
    @DisplayName("GET /api/v1/admin/users - returns 200 with user list including role and createdAt")
    void listAll_returnsAdminUserList() throws Exception {
        UUID userId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        UserEntity entity = new UserEntity();
        entity.setId(userId);
        entity.setUsername("user_a");
        entity.setRoles(Role.USER);

        AdminUserResponse adminResponse = new AdminUserResponse(userId, "user_a", Role.USER, now);

        when(userService.getAllUsers()).thenReturn(List.of(entity));
        when(userMapper.toAdminResponse(entity)).thenReturn(adminResponse);

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("user_a"))
                .andExpect(jsonPath("$[0].role").value("USER"))
                .andExpect(jsonPath("$[0].createdAt").exists());
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - returns empty array (not 404) when no users exist")
    void listAll_returnsEmptyArray_whenNoUsers() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/users/{id} - returns 204 for existing user")
    void remove_existingUser_returns204() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/v1/admin/users/{id}", userId))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/users/{id} - returns 404 for non-existent user")
    void remove_nonExistentUser_returns404() throws Exception {
        UUID userId = UUID.randomUUID();
        doThrow(new EntityNotFoundException("User not found: " + userId))
                .when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/v1/admin/users/{id}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/users/{id} - returns 400 for invalid UUID format")
    void remove_invalidUuid_returns400() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
