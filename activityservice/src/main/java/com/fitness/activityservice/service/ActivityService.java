package com.fitness.activityservice.service;

import com.fitness.activityservice.dto.request.ActivityRequest;
import com.fitness.activityservice.dto.response.ActivityResponse;
import com.fitness.activityservice.entity.Activity;
import com.fitness.activityservice.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityService {
    private final ActivityRepository repository;
    private final UserValidationService userValidationService;

    private ActivityResponse toResponse(Activity activity) {
        ActivityResponse response = new ActivityResponse();

        response.setId(activity.getId());
        response.setUserId(activity.getUserId());
        response.setType(activity.getType());
        response.setDuration(activity.getDuration());
        response.setCaloriesBurn(activity.getCaloriesBurn());
        response.setAdditionalMetrics(activity.getAdditionalMetrics());
        response.setStartTime(activity.getStartTime());
        response.setCreatedAt(activity.getCreatedAt());
        response.setUpdatedAt(activity.getUpdatedAt());

        return response;
    }

    public ActivityResponse trackActivity(ActivityRequest request) {
        boolean isValidUser = userValidationService.validateUser(request.getUserId());
        if (!isValidUser) {
            throw new RuntimeException("Invalid User: " + request.getUserId());
        }

        Activity activity = Activity.builder()
                .userId(request.getUserId())
                .type(request.getType())
                .duration(request.getDuration())
                .caloriesBurn(request.getCaloriesBurn())
                .startTime(request.getStartTime())
                .additionalMetrics(request.getAdditionalMetrics())
                .build();

        Activity savedActivity = repository.save(activity);

        return toResponse(savedActivity);
    }

    public List<ActivityResponse> getUserActivities(UUID userId) {
        List<Activity> activities = repository.findByUserId(userId);

        return activities.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ActivityResponse getActivity(UUID id) {
        return repository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RuntimeException("Activity Not found with id: " + id));
    }
}
