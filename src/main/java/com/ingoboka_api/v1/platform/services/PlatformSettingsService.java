package com.ingoboka_api.v1.platform.services;

import com.ingoboka_api.v1.common.requests.UpdatePlatformSettingsRequest;
import com.ingoboka_api.v1.common.responses.PlatformSettingsResponse;

public interface PlatformSettingsService {

    PlatformSettingsResponse getSettings();

    /** Public / runtime config used by clients after admin updates. */
    PlatformSettingsResponse getEffectiveConfig();

    PlatformSettingsResponse updateSettings(UpdatePlatformSettingsRequest request);

    void refreshCache();

    boolean isMaintenanceMode();

    boolean isRegistrationEnabled();
}
