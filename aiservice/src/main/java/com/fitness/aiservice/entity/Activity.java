package com.fitness.aiservice.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class Activity {
    private UUID id;
    private UUID userId;

    private ActivityType type;
    private Integer duration;
    private Integer caloriesBurn;

    private LocalDateTime startTime;

    private Map<String, Object> additionalMetrics;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
