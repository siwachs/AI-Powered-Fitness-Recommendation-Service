package com.fitness.gatewayservice.service;

import com.fitness.gatewayservice.dto.request.RegisterRequest;
import com.fitness.gatewayservice.dto.response.UserResponse;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final WebClient userServiceWebClient;

    public Mono<Boolean> validateUser(UUID userId) {
        log.info("Calling validateUser for userId: {}", userId);

        return userServiceWebClient.get()
                .uri("/api/v1/users/{userId}/validate", userId)
                .retrieve()
                .bodyToMono(Boolean.class)
                .onErrorResume(
                        WebClientResponseException.class,
                        e -> {
                            if (e.getStatusCode() == HttpStatus.NOT_FOUND)
                                return Mono.error(new RuntimeException("User Not Found: " + userId));
                            else if (e.getStatusCode() == HttpStatus.BAD_REQUEST)
                                return Mono.error(new RuntimeException("Invalid Request: " + userId));
                            return Mono.error(new RuntimeException("Unexpected Error: " + e.getMessage()));
                        }
                );
    }

    public RegisterRequest getUserDetails(String token) {
        try {
            String tokenWithoutBearer = token.replace("Bearer ", "").trim();
            SignedJWT signedJWT = SignedJWT.parse(tokenWithoutBearer);
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setEmail(claims.getStringClaim("email"));
            registerRequest.setKeycloakId(UUID.fromString(claims.getStringClaim("keycloakId")));
            registerRequest.setPassword("dummy@123123");
            registerRequest.setFirstName(claims.getStringClaim("given_name"));
            registerRequest.setLastName(claims.getStringClaim("family_name"));

            return registerRequest;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Mono<UserResponse> registerUser(RegisterRequest registerRequest) {
        log.info("Calling User Registration for userId: {}", registerRequest.getEmail());

        return userServiceWebClient.post()
                .uri("/api/v1/users/register")
                .bodyValue(registerRequest)
                .retrieve()
                .bodyToMono(UserResponse.class)
                .onErrorResume(
                        WebClientResponseException.class,
                        e -> {
                            if (e.getStatusCode() == HttpStatus.BAD_REQUEST)
                                return Mono.error(new RuntimeException("Bad Request: " + registerRequest.getEmail()));
                            else if (e.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR)
                                return Mono.error(new RuntimeException("Internal Server Error: " + e.getMessage()));
                            return Mono.error(new RuntimeException("Unexpected Error: " + e.getMessage()));
                        }
                );
    }
}
