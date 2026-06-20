package com.ingoboka_api.v1.common.responses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PolicyReportSummaryResponse {
    long activePolicies;
    long citizensEnrolled;
}
