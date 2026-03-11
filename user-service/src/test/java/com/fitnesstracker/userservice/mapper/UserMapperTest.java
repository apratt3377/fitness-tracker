package com.fitnesstracker.userservice.mapper;

import com.fitnesstracker.userservice.domain.UserEntity;
import com.fitnesstracker.userservice.dto.UserResponse;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void shouldMapEntityToResponseCorrectlly() {
        // Arrange
        UUID id = UUID.randomUUID();
        UserEntity entity = new UserEntity();
        entity.setId(id);
        entity.setUsername("user_a");
        entity.setPasswordHash("SHOULD_NOT_LEAK");

        // Act
        UserResponse result = userMapper.toResponse(entity);

        // Assert
        assertNotNull(result);
        assertEquals(id, result.id());
        assertEquals("user_a", result.username());
    }

    @Test
    void shouldReturnNullWhenEntityIsNull() {
        assertNull(userMapper.toResponse(null));
    }
}