package com.fitness.aiservice.config.gemini;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class GeminiConfig {
    private final GeminiProperties properties;

    @Bean
    public WebClient geminiWebClient() {
        return WebClient.builder()
                .baseUrl(properties.getApi().getUrl())
                .defaultHeader(
                        "x-goog-api-key",
                        properties.getApi().getKey())
                .build();
    }
}
