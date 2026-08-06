package com.ingoboka_api.v1.common.responses;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InsurerDashboardResponse {
    long activePolicies;
    long citizensEnrolled;
    long openClaims;
    long pendingApplications;
    long resolvedToday;
    double avgResolutionDays;
    List<StatusCount> claimsByStatus;
    List<NamedCount> enrollmentByProduct;
    List<GeographyCount> enrollmentByDistrict;

    @Value
    @Builder
    public static class StatusCount {
        String status;
        long count;
    }

    @Value
    @Builder
    public static class NamedCount {
        String name;
        long count;
    }

    @Value
    @Builder
    public static class GeographyCount {
        String district;
        String province;
        String districtCode;
        long enrolled;
    }
}
