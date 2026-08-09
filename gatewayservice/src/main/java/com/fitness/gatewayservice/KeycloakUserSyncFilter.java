package com.fitness.gatewayservice;

import com.fitness.gatewayservice.dto.request.RegisterRequest;
import com.fitness.gatewayservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserSyncFilter implements WebFilter {
    private final UserService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String userIdFromHeader = exchange.getRequest().getHeaders().getFirst("X-User-ID");
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (userIdFromHeader == null || token == null) {
            return chain.filter(exchange);
        }

        UUID userId;
        try {
            userId = UUID.fromString(userIdFromHeader);
        } catch (IllegalArgumentException e) {
            return Mono.error(new RuntimeException("Invalid UUID"));
        }

        return userService.validateUser(userId)
                .flatMap(
                        exists -> {
                            if (!exists) {
                                RegisterRequest registerRequest = userService.getUserDetails(token);
                                if (registerRequest != null) {
                                    return userService.registerUser(registerRequest).then();
                                } else {
                                    return Mono.empty();
                                }
                            } else {
                                log.info("User already exist, Skipping Sync.");
                                return Mono.empty();
                            }
                        }
                ).then(
                        Mono.defer(
                                () -> {
                                    ServerHttpRequest mutatedRequest = exchange
                                            .getRequest()
                                            .mutate()
                                            .header("X-User-ID", userIdFromHeader)
                                            .build();
                                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                                }
                        )
                );
    }
}
