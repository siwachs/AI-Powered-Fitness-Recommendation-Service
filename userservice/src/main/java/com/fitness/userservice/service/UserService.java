package com.fitness.userservice.service;

import com.fitness.userservice.dto.request.RegisterRequest;
import com.fitness.userservice.dto.response.UserResponse;
import com.fitness.userservice.entity.User;
import com.fitness.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository repository;

    private UserResponse toResponse(User user) {
        UserResponse response = new UserResponse();

        response.setId(user.getId());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());

        return response;
    }

    public UserResponse registerUser(RegisterRequest request) {
        if (repository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setEmail(registerRequest.getEmail());
        user.setPassword(registerRequest.getPassword());
        user.setFirstName(registerRequest.getFirstName());
        user.setLastName(registerRequest.getLastName());

        return toResponse(repository.save(user));
    }

    public UserResponse getUser(UUID userId) {
        return toResponse(
                repository.findById(userId)
                        .orElseThrow(() -> new RuntimeException("This user does not exist"))
        );
    }
}
