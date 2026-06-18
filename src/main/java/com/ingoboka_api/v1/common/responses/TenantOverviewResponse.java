package com.ingoboka_api.v1.common.responses;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TenantOverviewResponse {
    long activePolicies;
    long pendingApplications;
    long openClaims;
    long successfulPayments;
    BigDecimal pendingRevenue;
    BigDecimal settledRevenue;
}
