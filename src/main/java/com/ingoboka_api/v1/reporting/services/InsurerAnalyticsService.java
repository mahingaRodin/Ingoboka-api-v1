package com.ingoboka_api.v1.reporting.services;

import com.ingoboka_api.v1.common.responses.InsurerDashboardResponse;
import com.ingoboka_api.v1.common.responses.RevenueTrendResponse;

public interface InsurerAnalyticsService {

    InsurerDashboardResponse getDashboard();

    RevenueTrendResponse getRevenueTrends(String granularity);
}
