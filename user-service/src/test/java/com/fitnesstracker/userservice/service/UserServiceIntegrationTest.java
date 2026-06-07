package com.fitnesstracker.userservice.service;

import com.fitnesstracker.userservice.domain.Role;
import com.fitnesstracker.userservice.dto.UserCreateRequest;
import com.fitnesstracker.userservice.dto.UserPrincipalResponse;
import com.fitnesstracker.userservice.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("create → findById: persisted user is retrievable by ID")
    void createThenFindById_returnsUser() {
        var request = new UserCreateRequest("alice", "securePass1", "USER");
        var created = userService.createUser(request);

        var found = userService.findById(created.getId());

        assertTrue(found.isPresent());
        assertEquals("alice", found.get().getUsername());
        assertEquals(Role.USER, found.get().getRole());
        assertNotEquals("securePass1", found.get().getPasswordHash(), "Plain password must not be stored");
    }

    @Test
    @DisplayName("create → authenticate: BCrypt round-trip succeeds")
    void createThenAuthenticate_succeeds() {
        userService.createUser(new UserCreateRequest("bob", "myP@ssword1", "USER"));

        Optional<UserPrincipalResponse> principal = userService.authenticate("bob", "myP@ssword1");

        assertTrue(principal.isPresent());
        assertEquals("bob", principal.get().username());
        assertEquals("USER", principal.get().role());
    }

    @Test
    @DisplayName("authenticate: wrong password returns empty")
    void authenticate_wrongPassword_returnsEmpty() {
        userService.createUser(new UserCreateRequest("carol", "correctPass1", "USER"));

        assertTrue(userService.authenticate("carol", "wrongPass1").isEmpty());
    }

    @Test
    @DisplayName("authenticate: unknown username returns empty (timing protection active)")
    void authenticate_unknownUsername_returnsEmpty() {
        assertTrue(userService.authenticate("ghost_user", "anyPassword").isEmpty());
    }

    @Test
    @DisplayName("softDelete → findById: deleted user is invisible to queries")
    void softDelete_makesUserInvisible() {
        var created = userService.createUser(new UserCreateRequest("dave", "password12", "USER"));
        UUID id = created.getId();

        userService.deleteUser(id);

        assertTrue(userService.findById(id).isEmpty(), "Soft-deleted user must not be returned by findById");
        assertFalse(userService.getAllUsers().stream().anyMatch(u -> u.getId().equals(id)),
                "Soft-deleted user must not appear in getAllUsers");
    }

    @Test
    @DisplayName("softDelete → authenticate: deleted user cannot authenticate")
    void softDelete_preventsAuthentication() {
        userService.createUser(new UserCreateRequest("eve", "password12", "USER"));
        var created = userService.findById(
                userRepository.findByUsername("eve").orElseThrow().getId()
        ).orElseThrow();
        userService.deleteUser(created.getId());

        assertTrue(userService.authenticate("eve", "password12").isEmpty(),
                "Soft-deleted user must not authenticate");
    }

    @Test
    @DisplayName("deleteUser on non-existent ID throws EntityNotFoundException")
    void deleteUser_nonExistent_throwsNotFound() {
        assertThrows(EntityNotFoundException.class,
                () -> userService.deleteUser(UUID.randomUUID()));
    }
}
