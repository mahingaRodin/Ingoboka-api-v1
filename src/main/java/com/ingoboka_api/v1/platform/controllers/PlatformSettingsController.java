package com.ingoboka_api.v1.platform.controllers;

import com.ingoboka_api.v1.common.requests.UpdatePlatformSettingsRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.PlatformSettingsResponse;
import com.ingoboka_api.v1.platform.services.PlatformSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Platform Settings", description = "System-wide configuration managed by platform admins")
public class PlatformSettingsController {

    private final PlatformSettingsService platformSettingsService;

    @GetMapping("/api/v1/admin/platform/settings")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get platform settings")
    public ApiResponse<PlatformSettingsResponse> getSettings() {
        return ApiResponse.ok("Platform settings", platformSettingsService.getSettings());
    }

    @PutMapping("/api/v1/admin/platform/settings")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update platform settings (takes effect immediately)")
    public ApiResponse<PlatformSettingsResponse> updateSettings(
            @Valid @RequestBody UpdatePlatformSettingsRequest request) {
        return ApiResponse.ok("Platform settings updated", platformSettingsService.updateSettings(request));
    }

    @PostMapping("/api/v1/admin/platform/settings/refresh")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Force-refresh settings cache from database")
    public ApiResponse<PlatformSettingsResponse> refresh() {
        platformSettingsService.refreshCache();
        return ApiResponse.ok("Settings cache refreshed", platformSettingsService.getSettings());
    }

    /** Runtime config for clients — safe subset applied immediately after admin saves. */
    @GetMapping("/api/v1/platform/config")
    @Operation(summary = "Public effective platform configuration")
    public ApiResponse<PlatformSettingsResponse> publicConfig() {
        return ApiResponse.ok("Platform config", platformSettingsService.getEffectiveConfig());
    }
}
