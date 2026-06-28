package com.fitness.aiservice.service;

import com.fitness.aiservice.entity.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final RecommendationRepository recommendationRepository;

    public List<Recommendation> getUserRecommendation(UUID userId) {
        return recommendationRepository.findByUserId(userId);
    }

    public Recommendation getUserActivityRecommendation(UUID activityId) {
        return recommendationRepository.findByActivityId(activityId)
                .orElseThrow(() -> new RuntimeException("No Recommendation found for this activity: " + activityId));
    }
}
