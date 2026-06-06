package com.fitnesstracker.userservice.mapper;

import org.springframework.stereotype.Component;

import com.fitnesstracker.userservice.domain.UserEntity;
import com.fitnesstracker.userservice.dto.AdminUserResponse;
import com.fitnesstracker.userservice.dto.UserResponse;

@Component
public class UserMapper {

    public UserResponse toResponse(UserEntity user) {
        if (user == null) return null;
        return new UserResponse(user.getId(), user.getUsername());
    }

    public AdminUserResponse toAdminResponse(UserEntity user) {
        if (user == null) return null;
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getRoles(), user.getCreatedAt());
    }
}
