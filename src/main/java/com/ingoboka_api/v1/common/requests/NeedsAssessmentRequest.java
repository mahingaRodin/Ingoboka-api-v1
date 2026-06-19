package com.ingoboka_api.v1.common.requests;

import lombok.Data;

@Data
public class NeedsAssessmentRequest {

    private String occupation;
    private String incomeRange;
    private Integer dependents;
    private String primaryRisk;
}
