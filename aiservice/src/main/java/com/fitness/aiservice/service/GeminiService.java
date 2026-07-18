package com.fitness.aiservice.service;

import com.fitness.aiservice.dto.request.Content;
import com.fitness.aiservice.dto.request.GeminiRequest;
import com.fitness.aiservice.dto.request.Part;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GeminiService {
    private final WebClient geminiWebClient;

    public String getAnswer(String prompt) {
        GeminiRequest request = new GeminiRequest(
                List.of(
                        new Content(
                                List.of(
                                        new Part(prompt)
                                )
                        )
                )
        );

        return geminiWebClient.post()
                .uri("")
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
