package com.fitness.userservice.error;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
public class ValidationError {
    private Instant timestamp;
    private int status;
    private String error;
    private Map<String, String> fieldErrors;
    private String path;
}
