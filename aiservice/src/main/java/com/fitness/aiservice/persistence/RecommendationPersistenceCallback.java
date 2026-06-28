package com.fitness.aiservice.persistence;

import com.fitness.aiservice.entity.Recommendation;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RecommendationPersistenceCallback implements BeforeConvertCallback<Recommendation> {

    @Override
    public Recommendation onBeforeConvert(Recommendation recommendation, @NonNull String collection) {
        if (recommendation.getId() == null) {
            recommendation.setId(UUID.randomUUID());
        }

        return recommendation;
    }
}
