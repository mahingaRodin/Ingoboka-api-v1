package com.ingoboka_api.v1.common.responses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PlatformSettingsResponse {
    String platformName;
    String defaultLocale;
    boolean maintenanceMode;
    String apiBaseUrl;
    String supportEmail;
}
