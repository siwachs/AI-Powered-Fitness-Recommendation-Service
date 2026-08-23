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

@Component
@Slf4j
@RequiredArgsConstructor
public class KeycloakUserSyncFilter implements WebFilter {
    private final UserService userService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        RegisterRequest registerRequest = userService.getUserDetails(token);
        if (registerRequest == null || registerRequest.getKeycloakId() == null) {
            return chain.filter(exchange);
        }

        String keycloakId = registerRequest.getKeycloakId().toString();

        return userService.validateUser(registerRequest.getKeycloakId())
                .flatMap(exists -> exists
                        ? Mono.empty()
                        : userService.registerUser(registerRequest).then())
                .then(Mono.defer(() -> {
                    ServerHttpRequest mutatedRequest = exchange.getRequest()
                            .mutate()
                            .header("X-User-ID", keycloakId)
                            .build();

                    return chain.filter(exchange.mutate().request(mutatedRequest).build());
                }));
    }
}
