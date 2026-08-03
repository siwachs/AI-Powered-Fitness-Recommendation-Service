package com.fitness.userservice.service;

import com.fitness.userservice.dto.request.RegisterRequest;
import com.fitness.userservice.dto.response.UserResponse;
import com.fitness.userservice.entity.User;
import com.fitness.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository repository;

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();

        response.setId(user.getId());
        response.setKeycloakId(user.getKeycloakId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        return response;
    }

    public UserResponse registerUser(RegisterRequest request) {
        Optional<User> existingUser = repository.findByEmail(request.getEmail());
        if (existingUser.isPresent()) {
            return toResponse(existingUser.get());
        }

        User user = new User();
        user.setKeycloakId(request.getKeycloakId());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        return toResponse(repository.save(user));
    }

    public UserResponse getUser(UUID userId) {
        return toResponse(
                repository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("This user does not exist"))
        );
    }

    public Boolean existByKeycloakId(UUID keycloakId) {
        log.info("Calling User validation API for keycloakId: {}", keycloakId);
        return repository.existsByKeycloakId(keycloakId);
    }
}
