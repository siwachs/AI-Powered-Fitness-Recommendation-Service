package com.fitness.aiservice.controller;

import com.fitness.aiservice.entity.Recommendation;
import com.fitness.aiservice.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendationService;

    @GetMapping("user/{userId}")
    public ResponseEntity<List<Recommendation>> getUserRecommendation(@PathVariable UUID userId) {
        return ResponseEntity.ok(recommendationService.getUserRecommendation(userId));
    }

    @GetMapping("activity/{activityId}")
    public ResponseEntity<Recommendation> getUserActivityRecommendation(@PathVariable UUID activityId) {
        return ResponseEntity.ok(recommendationService.getUserActivityRecommendation(activityId));
    }
}
