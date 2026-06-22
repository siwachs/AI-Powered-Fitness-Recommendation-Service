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

    public boolean validateUser(UUID userId) {
        log.info("Calling validateUser for userId: {}", userId);
        try {
            return Boolean.TRUE.equals(userServiceWebClient.get()
                    .uri("/api/v1/users/{userId}/validate", userId)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block()); // WebClient = Reactive and block() = Blocking
        } catch (WebClientResponseException.NotFound e) {
            throw new RuntimeException("User Not Found: " + userId);
        } catch (WebClientResponseException.BadRequest e) {
            throw new RuntimeException("Invalid Request: " + userId);
        }
    }
}
