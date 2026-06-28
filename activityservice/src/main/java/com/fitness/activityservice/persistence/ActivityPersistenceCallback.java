package com.fitness.activityservice.persistence;

import com.fitness.activityservice.entity.Activity;
import org.springframework.data.mongodb.core.mapping.event.BeforeConvertCallback;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ActivityPersistenceCallback implements BeforeConvertCallback<Activity> {

    @Override
    public Activity onBeforeConvert(Activity activity, @NonNull String collection) {
        if (activity.getId() == null) {
            activity.setId(UUID.randomUUID());
        }

        return activity;
    }
}
