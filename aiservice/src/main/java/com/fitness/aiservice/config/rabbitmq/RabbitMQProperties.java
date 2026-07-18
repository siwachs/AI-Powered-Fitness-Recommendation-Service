package com.fitness.aiservice.config.rabbitmq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.rabbitmq")
public class RabbitMQProperties {
    private Exchanges exchanges;
    private Queues queues;
    private RoutingKeys routingKeys;

    @Data
    public static class Exchanges {
        private String fitness;
    }

    @Data
    public static class Queues {
        private String activity;
    }

    @Data
    public static class RoutingKeys {
        private String activity;
    }
}
