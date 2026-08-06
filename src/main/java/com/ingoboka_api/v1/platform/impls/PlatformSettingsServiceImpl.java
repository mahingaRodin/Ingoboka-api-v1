package com.ingoboka_api.v1.platform.impls;

import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import com.ingoboka_api.v1.common.requests.UpdatePlatformSettingsRequest;
import com.ingoboka_api.v1.common.responses.PlatformSettingsResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.platform.models.PlatformSetting;
import com.ingoboka_api.v1.platform.repositories.PlatformSettingRepository;
import com.ingoboka_api.v1.platform.services.AnnouncementService;
import com.ingoboka_api.v1.platform.services.PlatformSettingsService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PlatformSettingsServiceImpl implements PlatformSettingsService {

    private final PlatformSettingRepository platformSettingRepository;
    private final AuditComplianceService auditComplianceService;
    private final AnnouncementService announcementService;

    @Value("${ingoboka.platform.name:Ingoboka}")
    private String defaultPlatformName;

    @Value("${ingoboka.platform.default-locale:rw}")
    private String defaultLocaleProp;

    @Value("${ingoboka.platform.maintenance-mode:false}")
    private boolean defaultMaintenanceMode;

    @Value("${ingoboka.platform.api-base-url:/api/v1}")
    private String defaultApiBaseUrl;

    @Value("${ingoboka.platform.support-email:support@ingoboka.rw}")
    private String defaultSupportEmail;

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    private volatile Instant cacheLoadedAt = Instant.EPOCH;

    @Override
    @Transactional(readOnly = true)
    public PlatformSettingsResponse getSettings() {
        ensureCache();
        return toResponse();
    }

    @Override
    @Transactional(readOnly = true)
    public PlatformSettingsResponse getEffectiveConfig() {
        return getSettings();
    }

    @Override
    @Transactional
    public PlatformSettingsResponse updateSettings(UpdatePlatformSettingsRequest request) {
        UUID actorId = null;
        try {
            actorId = SecurityUtils.currentUser().getUserId();
        } catch (Exception ignored) {
            // system context
        }
        Instant now = Instant.now();
        Map<String, String> updates = new HashMap<>();

        putIfPresent(updates, "platformName", request.getPlatformName());
        putIfPresent(updates, "defaultLocale", request.getDefaultLocale());
        putIfPresent(updates, "maintenanceMode", request.getMaintenanceMode());
        putIfPresent(updates, "apiBaseUrl", request.getApiBaseUrl());
        putIfPresent(updates, "supportEmail", request.getSupportEmail());
        putIfPresent(updates, "supportPhone", request.getSupportPhone());
        putIfPresent(updates, "brandingTagline", request.getBrandingTagline());
        putIfPresent(updates, "registrationEnabled", request.getRegistrationEnabled());
        putIfPresent(updates, "selfServiceClaimsEnabled", request.getSelfServiceClaimsEnabled());
        putIfPresent(updates, "emailNotificationsEnabled", request.getEmailNotificationsEnabled());
        putIfPresent(updates, "smsNotificationsEnabled", request.getSmsNotificationsEnabled());
        putIfPresent(updates, "ussdEnabled", request.getUssdEnabled());
        putIfPresent(updates, "agentAssistedEnabled", request.getAgentAssistedEnabled());
        putIfPresent(updates, "requireKycBeforeEnrollment", request.getRequireKycBeforeEnrollment());
        putIfPresent(updates, "defaultCurrency", request.getDefaultCurrency());
        putIfPresent(updates, "defaultPolicyGraceDays", request.getDefaultPolicyGraceDays());
        putIfPresent(updates, "maxLoginAttempts", request.getMaxLoginAttempts());
        putIfPresent(updates, "apiRateLimitPerMinute", request.getApiRateLimitPerMinute());

        for (Map.Entry<String, String> entry : updates.entrySet()) {
            PlatformSetting row = platformSettingRepository.findById(entry.getKey()).orElseGet(PlatformSetting::new);
            row.setSettingKey(entry.getKey());
            row.setSettingValue(entry.getValue());
            row.setUpdatedAt(now);
            row.setUpdatedBy(actorId);
            platformSettingRepository.save(row);
            cache.put(entry.getKey(), entry.getValue());
        }
        cacheLoadedAt = now;

        auditComplianceService.log(
                "PLATFORM_SETTINGS_UPDATED",
                "PLATFORM_SETTINGS",
                null,
                "Updated keys: " + String.join(", ", updates.keySet()));

        if (!updates.isEmpty()) {
            announcementService.createPlatformAnnouncement(
                    "Platform settings updated",
                    "Important platform settings were changed. Review notifications for details that may affect your account.");
        }

        return toResponse();
    }

    @Override
    @Transactional(readOnly = true)
    public void refreshCache() {
        cache.clear();
        cacheLoadedAt = Instant.EPOCH;
        ensureCache();
    }

    @Override
    public boolean isMaintenanceMode() {
        ensureCache();
        return Boolean.parseBoolean(cache.getOrDefault("maintenanceMode", String.valueOf(defaultMaintenanceMode)));
    }

    @Override
    public boolean isRegistrationEnabled() {
        ensureCache();
        return Boolean.parseBoolean(cache.getOrDefault("registrationEnabled", "true"));
    }

    private void ensureCache() {
        if (!cache.isEmpty() && cacheLoadedAt.isAfter(Instant.EPOCH)) {
            return;
        }
        synchronized (cache) {
            if (!cache.isEmpty() && cacheLoadedAt.isAfter(Instant.EPOCH)) {
                return;
            }
            cache.clear();
            Instant latest = Instant.EPOCH;
            for (PlatformSetting row : platformSettingRepository.findAll()) {
                cache.put(row.getSettingKey(), row.getSettingValue());
                if (row.getUpdatedAt() != null && row.getUpdatedAt().isAfter(latest)) {
                    latest = row.getUpdatedAt();
                }
            }
            seedDefaultsIntoCache();
            cacheLoadedAt = latest.isAfter(Instant.EPOCH) ? latest : Instant.now();
        }
    }

    private void seedDefaultsIntoCache() {
        cache.putIfAbsent("platformName", defaultPlatformName);
        cache.putIfAbsent("defaultLocale", defaultLocaleProp);
        cache.putIfAbsent("maintenanceMode", String.valueOf(defaultMaintenanceMode));
        cache.putIfAbsent("apiBaseUrl", defaultApiBaseUrl);
        cache.putIfAbsent("supportEmail", defaultSupportEmail);
        cache.putIfAbsent("supportPhone", "+250788000000");
        cache.putIfAbsent("brandingTagline", "Digital microinsurance for Rwanda");
        cache.putIfAbsent("registrationEnabled", "true");
        cache.putIfAbsent("selfServiceClaimsEnabled", "true");
        cache.putIfAbsent("emailNotificationsEnabled", "true");
        cache.putIfAbsent("smsNotificationsEnabled", "true");
        cache.putIfAbsent("ussdEnabled", "true");
        cache.putIfAbsent("agentAssistedEnabled", "true");
        cache.putIfAbsent("requireKycBeforeEnrollment", "false");
        cache.putIfAbsent("defaultCurrency", "RWF");
        cache.putIfAbsent("defaultPolicyGraceDays", "7");
        cache.putIfAbsent("maxLoginAttempts", "5");
        cache.putIfAbsent("apiRateLimitPerMinute", "120");
    }

    private PlatformSettingsResponse toResponse() {
        return PlatformSettingsResponse.builder()
                .platformName(cache.getOrDefault("platformName", defaultPlatformName))
                .defaultLocale(cache.getOrDefault("defaultLocale", defaultLocaleProp))
                .maintenanceMode(Boolean.parseBoolean(cache.getOrDefault("maintenanceMode", "false")))
                .apiBaseUrl(cache.getOrDefault("apiBaseUrl", defaultApiBaseUrl))
                .supportEmail(cache.getOrDefault("supportEmail", defaultSupportEmail))
                .supportPhone(cache.getOrDefault("supportPhone", "+250788000000"))
                .brandingTagline(cache.getOrDefault("brandingTagline", "Digital microinsurance for Rwanda"))
                .registrationEnabled(Boolean.parseBoolean(cache.getOrDefault("registrationEnabled", "true")))
                .selfServiceClaimsEnabled(
                        Boolean.parseBoolean(cache.getOrDefault("selfServiceClaimsEnabled", "true")))
                .emailNotificationsEnabled(
                        Boolean.parseBoolean(cache.getOrDefault("emailNotificationsEnabled", "true")))
                .smsNotificationsEnabled(
                        Boolean.parseBoolean(cache.getOrDefault("smsNotificationsEnabled", "true")))
                .ussdEnabled(Boolean.parseBoolean(cache.getOrDefault("ussdEnabled", "true")))
                .agentAssistedEnabled(Boolean.parseBoolean(cache.getOrDefault("agentAssistedEnabled", "true")))
                .requireKycBeforeEnrollment(
                        Boolean.parseBoolean(cache.getOrDefault("requireKycBeforeEnrollment", "false")))
                .defaultCurrency(cache.getOrDefault("defaultCurrency", "RWF"))
                .defaultPolicyGraceDays(parseInt(cache.get("defaultPolicyGraceDays"), 7))
                .maxLoginAttempts(parseInt(cache.get("maxLoginAttempts"), 5))
                .apiRateLimitPerMinute(parseInt(cache.get("apiRateLimitPerMinute"), 120))
                .updatedAt(cacheLoadedAt)
                .build();
    }

    private static void putIfPresent(Map<String, String> target, String key, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof String s && !StringUtils.hasText(s)) {
            return;
        }
        target.put(key, String.valueOf(value));
    }

    private static int parseInt(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
