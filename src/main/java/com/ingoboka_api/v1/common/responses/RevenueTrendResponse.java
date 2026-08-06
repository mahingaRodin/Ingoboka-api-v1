package com.ingoboka_api.v1.common.responses;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RevenueTrendResponse {
    String granularity;
    BigDecimal totalRevenue;
    BigDecimal totalSettled;
    BigDecimal totalPending;
    BigDecimal totalSpending;
    List<PeriodPoint> periods;

    @Value
    @Builder
    public static class PeriodPoint {
        String label;
        String periodStart;
        String periodEnd;
        BigDecimal revenue;
        BigDecimal settled;
        BigDecimal pending;
        BigDecimal spending;
        long invoiceCount;
    }
}
