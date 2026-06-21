package com.fitness.activityservice.repository;

import com.fitness.activityservice.entity.Activity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityRepository extends MongoRepository<Activity, UUID> {
    List<Activity> findByUserId(UUID userId);
}
