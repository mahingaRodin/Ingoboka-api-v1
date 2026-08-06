package com.ingoboka_api.v1.common.requests;

import java.util.Map;
import lombok.Data;

@Data
public class SaveNeedsAssessmentPreferencesRequest {

    private String occupation;
    private String incomeRange;
    private Integer dependents;
    private String primaryRisk;
    private String paymentPreference;
    private String smartphoneAccess;
    private Map<String, Object> answers;
}
