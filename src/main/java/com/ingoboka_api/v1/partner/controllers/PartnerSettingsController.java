package com.ingoboka_api.v1.partner.controllers;

import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.UpdateOrganizationSettingsRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.OrganizationSettingsResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.partner.models.OrganizationSettings;
import com.ingoboka_api.v1.partner.repositories.OrganizationSettingsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/insurer/settings", "/api/v1/insurer/settings", "/api/partners/me/settings"})
@RequiredArgsConstructor
@Tag(name = "Insurer Settings", description = "Tenant organization configuration")
@SecurityRequirement(name = "bearerAuth")
public class PartnerSettingsController {

    private final OrganizationSettingsRepository organizationSettingsRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Get organization settings JSON")
    public ApiResponse<OrganizationSettingsResponse> getSettings() {
        OrganizationSettings settings = requireSettings();
        return ApiResponse.ok("Settings retrieved", toResponse(settings));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Update organization settings JSON")
    public ApiResponse<OrganizationSettingsResponse> updateSettings(
            @Valid @RequestBody UpdateOrganizationSettingsRequest request) {
        OrganizationSettings settings = requireSettings();
        settings.setSettingsJson(request.getSettingsJson());
        settings.setUpdatedAt(Instant.now());
        organizationSettingsRepository.save(settings);
        return ApiResponse.ok("Settings updated", toResponse(settings));
    }

    private OrganizationSettings requireSettings() {
        UUID orgId = SecurityUtils.currentUser().getOrganizationId();
        if (orgId == null) {
            throw new BusinessException("No organization associated with this account");
        }
        return organizationSettingsRepository
                .findByOrganizationId(orgId)
                .orElseGet(() -> {
                    Instant now = Instant.now();
                    OrganizationSettings created = new OrganizationSettings();
                    created.setId(UUID.randomUUID());
                    created.setOrganizationId(orgId);
                    created.setSettingsJson("{}");
                    created.setCreatedAt(now);
                    created.setUpdatedAt(now);
                    return organizationSettingsRepository.save(created);
                });
    }

    private OrganizationSettingsResponse toResponse(OrganizationSettings settings) {
        return OrganizationSettingsResponse.builder()
                .organizationId(settings.getOrganizationId())
                .settingsJson(settings.getSettingsJson())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
