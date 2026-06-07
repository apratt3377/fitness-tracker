package com.fitnesstracker.userservice.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fitnesstracker.userservice.domain.Role;
import com.fitnesstracker.userservice.domain.UserEntity;
import com.fitnesstracker.userservice.dto.UserCreateRequest;
import com.fitnesstracker.userservice.dto.UserPrincipalResponse;
import com.fitnesstracker.userservice.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    // Pre-computed BCrypt(strength=10) hash used for dummy comparisons when a username does not
    // exist, preventing timing-based username enumeration. Same pattern as Spring Security's
    // DaoAuthenticationProvider.userNotFoundEncodedPassword.
    private static final String DUMMY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> findById(UUID id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<UserPrincipalResponse> authenticate(String username, String plainPassword) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            passwordEncoder.matches(plainPassword, DUMMY_HASH);
            return Optional.empty();
        }
        UserEntity user = userOpt.get();
        if (!passwordEncoder.matches(plainPassword, user.getPasswordHash())) {
            return Optional.empty();
        }
        return Optional.of(new UserPrincipalResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        ));
    }

    @Transactional
    public UserEntity createUser(UserCreateRequest request) {
        UserEntity newUser = new UserEntity();
        newUser.setUsername(request.username());
        newUser.setPasswordHash(passwordEncoder.encode(request.password()));
        Role assignedRole = Arrays.stream(Role.values())
                .filter(r -> r.name().equalsIgnoreCase(request.role()))
                .findFirst()
                .orElse(Role.USER);
        newUser.setRole(assignedRole);
        return userRepository.save(newUser);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new EntityNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }
}
