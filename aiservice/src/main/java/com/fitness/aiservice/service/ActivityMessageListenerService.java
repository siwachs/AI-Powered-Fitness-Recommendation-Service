package com.fitness.aiservice.service;

import com.fitness.aiservice.entity.Activity;
import com.fitness.aiservice.entity.Recommendation;
import com.fitness.aiservice.repository.RecommendationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActivityMessageListenerService {
    private final ActivityAiService activityAiService;
    private final RecommendationRepository recommendationRepository;

    @RabbitListener(queues = "${app.rabbitmq.queues.activity}")
    public void processActivity(Activity activity) {
        log.info("Received activity for processing: {}", activity.getId());
        // log.info("Generated Recommendation: {}", activityAiService.generateRecommendation(activity));

        Recommendation recommendation = activityAiService.generateRecommendation(activity);
        recommendationRepository.save(recommendation);
    }
}
