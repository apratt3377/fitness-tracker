package com.fitnesstracker.userservice.mapper;

import org.springframework.stereotype.Component;

import com.fitnesstracker.userservice.domain.UserEntity;
import com.fitnesstracker.userservice.dto.UserResponse;

/**
 * Mapper utility to convert between Domain Entities and API DTOs.
 * Supports Resiliency by decoupling the DB schema from the API contract.
 */
@Component
public class UserMapper {

    /**
     * Maps a User domain entity to a UserResponseDTO.
     * This explicitly excludes sensitive fields like the password hash.
     */
    public UserResponse toResponse(UserEntity user) {
        if (user == null) {
            return null;
        }

        return new UserResponse(
            user.getId(),       // Maps UUID from 'account' table
            user.getUsername() // Maps varchar(50)
        );
    }
}
