package com.fitness.activityservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationService {
    private final WebClient userServiceWebClient;

    public boolean validateUser(UUID keycloakId) {
        log.info("Calling validateUser for userId: {}", keycloakId);
        try {
            return Boolean.TRUE.equals(userServiceWebClient.get()
                    .uri("/api/v1/users/{ keycloakId}/validate", keycloakId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block()); // WebClient = Reactive and block() = Blocking
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException("User Not Found: " + keycloakId);
        } catch (WebClientResponseException.BadRequest e) {
            throw new RuntimeException("Invalid Request: " + keycloakId);
        }
    }
}
