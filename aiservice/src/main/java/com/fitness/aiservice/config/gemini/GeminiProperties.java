package com.fitness.aiservice.config.gemini;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("app.gemini")
public class GeminiProperties {
    private API api;

    @Data
    public static class API {
        private String url;
        private String key;
    }
}
