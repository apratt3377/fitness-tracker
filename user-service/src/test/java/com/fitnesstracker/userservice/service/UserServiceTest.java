package com.fitnesstracker.userservice.service;

import com.fitnesstracker.userservice.domain.UserEntity;
import com.fitnesstracker.userservice.dto.UserCreateRequest;
import com.fitnesstracker.userservice.dto.UserPrincipalResponse;
import com.fitnesstracker.userservice.repository.UserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository userDao;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("authenticate() - Should return Principal when credentials match")
    void authenticateSuccess() {
        // Arrange
        String username = "user_a";
        String pass = "password123";
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setPasswordHash("hashed_string");
        user.setRoles(UserEntity.Role.USER);

        when(userDao.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(pass, "hashed_string")).thenReturn(true);

        // Act
        Optional<UserPrincipalResponse> result = userService.authenticate(username, pass);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(username, result.get().username());
        assertEquals(UserEntity.Role.USER, result.get().roles());
    }

    

    @Test
    @DisplayName("authenticate() - Should return empty when password is wrong")
    void authenticateWrongPassword() {
        // Arrange
        String username = "user_a";
        UserEntity user = new UserEntity();
        user.setPasswordHash("correct_hash");

        when(userDao.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq("correct_hash"))).thenReturn(false);

        // Act
        Optional<UserPrincipalResponse> result = userService.authenticate(username, "wrong_pass");

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("createUser() - Should hash password and default to USER role")
    void createUserLogic() {
        // Arrange
        UserCreateRequest request = new UserCreateRequest("newbie", "raw_pass", "INVALID_ROLE");
        when(passwordEncoder.encode("raw_pass")).thenReturn("hashed_pass");
        
        // Use a spy or capture to verify the entity state before saving
        when(userDao.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        UserEntity savedUser = userService.createUser(request);

        // Assert
        assertEquals("hashed_pass", savedUser.getPasswordHash());
        assertEquals(UserEntity.Role.USER, savedUser.getRoles(), "Should fallback to USER for invalid roles");
        verify(userDao, times(1)).save(any());
    }

    @Test
    @DisplayName("deleteUser() - Should invoke DAO delete")
    void deleteUserInvokesDao() {
        UUID id = UUID.randomUUID();
        doNothing().when(userDao).deleteById(id);

        userService.deleteUser(id);

        verify(userDao, times(1)).deleteById(id);
    }

    @Test
    @DisplayName("findById() - Should return user when exists")
    void findByIdSuccess() {
        // Arrange
        UUID id = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(id);
        
        when(userDao.findById(id)).thenReturn(Optional.of(user));

        // Act
        Optional<UserEntity> result = userService.findById(id);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        verify(userDao, times(1)).findById(id);
    }

    @Test
    @DisplayName("getAllUsers() - Should return list of users")
    void getAllUsersSuccess() {
        // Arrange
        UserEntity user1 = new UserEntity();
        UserEntity user2 = new UserEntity();
        when(userDao.findAll()).thenReturn(java.util.List.of(user1, user2));

        // Act
        Iterable<UserEntity> result = userService.getAllUsers();

        // Assert
        assertNotNull(result);
        long count = java.util.stream.StreamSupport.stream(result.spliterator(), false).count();
        assertEquals(2, count);
        verify(userDao, times(1)).findAll();
    }
}