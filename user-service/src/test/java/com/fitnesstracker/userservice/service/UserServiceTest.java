package com.fitnesstracker.userservice.service;

import com.fitnesstracker.userservice.domain.Role;
import com.fitnesstracker.userservice.domain.UserEntity;
import com.fitnesstracker.userservice.dto.UserCreateRequest;
import com.fitnesstracker.userservice.dto.UserPrincipalResponse;
import com.fitnesstracker.userservice.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("authenticate() - returns principal when credentials match")
    void authenticate_validCredentials_returnsPrincipal() {
        String username = "user_a";
        String plainPass = "password123";
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername(username);
        user.setPasswordHash("hashed_string");
        user.setRoles(Role.USER);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(plainPass, "hashed_string")).thenReturn(true);

        Optional<UserPrincipalResponse> result = userService.authenticate(username, plainPass);

        assertTrue(result.isPresent());
        assertEquals(username, result.get().username());
        assertEquals(List.of("USER"), result.get().roles());
    }

    @Test
    @DisplayName("authenticate() - returns empty when password is wrong")
    void authenticate_wrongPassword_returnsEmpty() {
        String username = "user_a";
        UserEntity user = new UserEntity();
        user.setPasswordHash("correct_hash");

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq("correct_hash"))).thenReturn(false);

        Optional<UserPrincipalResponse> result = userService.authenticate(username, "wrong_pass");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("authenticate() - performs dummy BCrypt when username does not exist to prevent timing enumeration")
    void authenticate_unknownUsername_runsDummyBcryptAndReturnsEmpty() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        Optional<UserPrincipalResponse> result = userService.authenticate("ghost", "anyPass");

        assertFalse(result.isPresent());
        // Verify BCrypt was still called — prevents timing side-channel leaking username existence
        verify(passwordEncoder, times(1)).matches(eq("anyPass"), anyString());
    }

    @Test
    @DisplayName("createUser() - hashes password and defaults to USER role for null role input")
    void createUser_nullRole_defaultsToUser() {
        UserCreateRequest request = new UserCreateRequest("newbie", "rawPassword", null);
        when(passwordEncoder.encode("rawPassword")).thenReturn("hashed_pass");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserEntity saved = userService.createUser(request);

        assertEquals("hashed_pass", saved.getPasswordHash());
        assertEquals(Role.USER, saved.getRoles());
        verify(userRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("createUser() - assigns ADMIN role when explicitly specified")
    void createUser_adminRole_assignsAdmin() {
        UserCreateRequest request = new UserCreateRequest("adminUser", "rawPassword", "ADMIN");
        when(passwordEncoder.encode(any())).thenReturn("hashed_pass");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        UserEntity saved = userService.createUser(request);

        assertEquals(Role.ADMIN, saved.getRoles());
    }

    @Test
    @DisplayName("findById() - returns user when found")
    void findById_found_returnsUser() {
        UUID id = UUID.randomUUID();
        UserEntity user = new UserEntity();
        user.setId(id);

        when(userRepository.findById(id)).thenReturn(Optional.of(user));

        Optional<UserEntity> result = userService.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    @DisplayName("findById() - returns empty when not found")
    void findById_notFound_returnsEmpty() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertTrue(userService.findById(id).isEmpty());
    }

    @Test
    @DisplayName("getAllUsers() - returns list of users")
    void getAllUsers_returnsAll() {
        when(userRepository.findAll()).thenReturn(List.of(new UserEntity(), new UserEntity()));

        assertEquals(2, userService.getAllUsers().size());
    }

    @Test
    @DisplayName("deleteUser() - calls deleteById when user exists")
    void deleteUser_existingUser_invokesDelete() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(true);
        doNothing().when(userRepository).deleteById(id);

        userService.deleteUser(id);

        verify(userRepository, times(1)).deleteById(id);
    }

    @Test
    @DisplayName("deleteUser() - throws EntityNotFoundException when user does not exist")
    void deleteUser_nonExistentUser_throwsNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.existsById(id)).thenReturn(false);

        assertThrows(EntityNotFoundException.class, () -> userService.deleteUser(id));
        verify(userRepository, never()).deleteById(any());
    }
}
