package com.fitness.aiservice.repository;

import com.fitness.aiservice.entity.Recommendation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecommendationRepository extends MongoRepository<Recommendation, UUID> {
    List<Recommendation> findByUserId(UUID userId);

    Optional<Recommendation> findByActivityId(UUID activityId);
}
