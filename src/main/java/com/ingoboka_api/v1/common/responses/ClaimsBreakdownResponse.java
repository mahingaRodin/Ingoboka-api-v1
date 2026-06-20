package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.ClaimStatus;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClaimsBreakdownResponse {
    long resolvedToday;
    double avgResolutionDays;
    List<StatusCount> claimsByStatus;

    @Value
    @Builder
    public static class StatusCount {
        ClaimStatus status;
        long count;
    }
}
