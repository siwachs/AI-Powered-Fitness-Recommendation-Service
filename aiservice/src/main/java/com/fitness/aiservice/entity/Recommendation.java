package com.fitness.aiservice.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Document(collection = "recommendations")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Recommendation {
    @Id
    private UUID id;
    private UUID activityId;
    private UUID userId;

    private ActivityType activityType;
    private String recommendation;
    private List<String> improvements;
    private List<String> suggestions;
    private List<String> safety;

    @CreatedDate
    private LocalDateTime createdAt;
}
