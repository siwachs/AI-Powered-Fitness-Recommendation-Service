package com.fitness.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class RegisterRequest {
    private UUID keycloakId;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid Email format")
    private String email;

    private String firstName;
    private String lastName;
}
