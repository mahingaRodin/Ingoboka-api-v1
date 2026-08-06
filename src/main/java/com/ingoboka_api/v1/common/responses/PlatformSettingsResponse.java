package com.ingoboka_api.v1.common.responses;

import java.time.Instant;
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
    String supportPhone;
    String brandingTagline;
    boolean registrationEnabled;
    boolean selfServiceClaimsEnabled;
    boolean emailNotificationsEnabled;
    boolean smsNotificationsEnabled;
    boolean ussdEnabled;
    boolean agentAssistedEnabled;
    boolean requireKycBeforeEnrollment;
    String defaultCurrency;
    int defaultPolicyGraceDays;
    int maxLoginAttempts;
    int apiRateLimitPerMinute;
    Instant updatedAt;
}
