package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdatePlatformSettingsRequest {

    @Size(max = 120)
    private String platformName;

    @Size(max = 16)
    private String defaultLocale;

    private Boolean maintenanceMode;

    @Size(max = 255)
    private String apiBaseUrl;

    @Email
    @Size(max = 320)
    private String supportEmail;

    @Size(max = 40)
    private String supportPhone;

    @Size(max = 255)
    private String brandingTagline;

    private Boolean registrationEnabled;
    private Boolean selfServiceClaimsEnabled;
    private Boolean emailNotificationsEnabled;
    private Boolean smsNotificationsEnabled;
    private Boolean ussdEnabled;
    private Boolean agentAssistedEnabled;
    private Boolean requireKycBeforeEnrollment;

    @Size(max = 8)
    private String defaultCurrency;

    @Min(0)
    @Max(90)
    private Integer defaultPolicyGraceDays;

    @Min(1)
    @Max(50)
    private Integer maxLoginAttempts;

    @Min(10)
    @Max(10000)
    private Integer apiRateLimitPerMinute;
}
