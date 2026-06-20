package com.fitness.userservice.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class UserResponse {
    private UUID id;

    private String email;

    private String firstName;
    private String lastName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
