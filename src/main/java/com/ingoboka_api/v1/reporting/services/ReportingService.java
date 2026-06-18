package com.ingoboka_api.v1.reporting.services;

import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.TenantOverviewResponse;

public interface ReportingService {

    TenantOverviewResponse getTenantOverview();

    PageResponse<?> getPolicyReport(int page, int size);

    PageResponse<?> getClaimReport(int page, int size);

    PageResponse<?> getPaymentReport(int page, int size);

    String exportPoliciesCsv();

    String exportClaimsCsv();

    String exportPaymentsCsv();
}
