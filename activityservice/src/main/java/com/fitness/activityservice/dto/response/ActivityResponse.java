package com.fitness.activityservice.dto.response;

import com.fitness.activityservice.entity.ActivityType;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
public class ActivityResponse {
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
