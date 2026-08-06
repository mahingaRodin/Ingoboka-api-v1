package com.ingoboka_api.v1.common.responses;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NeedsAssessmentPreferencesResponse {
    boolean completed;
    Instant completedAt;
    Map<String, Object> preferences;
}
