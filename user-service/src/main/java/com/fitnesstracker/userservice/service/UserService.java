package com.fitnesstracker.userservice.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fitnesstracker.userservice.domain.UserEntity;
import com.fitnesstracker.userservice.dto.UserCreateRequest;
import com.fitnesstracker.userservice.dto.UserPrincipalResponse;
import com.fitnesstracker.userservice.repository.UserRepository;

import jakarta.transaction.Transactional;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<UserEntity> findById(UUID id) {
        return userRepository.findById(id);
    }

    public Optional<UserPrincipalResponse> authenticate(String username, String plainPassword) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(plainPassword, user.getPasswordHash()))
                .map(user -> new UserPrincipalResponse(
                        user.getId(), 
                        user.getUsername(), 
                        user.getRoles()
                ));
    }

@Transactional
    public UserEntity createUser(UserCreateRequest request) {
        UserEntity newUser = new UserEntity();
        newUser.setUsername(request.username());
        String hashedPassword = passwordEncoder.encode(request.password());
        newUser.setPasswordHash(hashedPassword);
        UserEntity.Role assignedRole = Arrays.stream(UserEntity.Role.values())
                .filter(r -> r.name().equalsIgnoreCase(request.roles()))
                .findFirst()
                .orElse(UserEntity.Role.USER);
        newUser.setRoles(assignedRole);

        return userRepository.save(newUser);
    }

    public Iterable<UserEntity> getAllUsers() {
        return userRepository.findAll();
    }

    public void deleteUser(UUID id) {
        userRepository.deleteById(id);
    }
}
