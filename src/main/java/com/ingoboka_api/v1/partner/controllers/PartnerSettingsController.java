package com.ingoboka_api.v1.partner.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.UpdateOrganizationSettingsRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.OrganizationSettingsResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.identity.models.Organization;
import com.ingoboka_api.v1.identity.repositories.OrganizationRepository;
import com.ingoboka_api.v1.partner.models.OrganizationSettings;
import com.ingoboka_api.v1.partner.models.PartnerProfile;
import com.ingoboka_api.v1.partner.repositories.OrganizationSettingsRepository;
import com.ingoboka_api.v1.partner.repositories.PartnerProfileRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/insurer/settings")
@RequiredArgsConstructor
@Tag(name = "Insurer Settings", description = "Tenant organization configuration")
@SecurityRequirement(name = "bearerAuth")
public class PartnerSettingsController {

    private final OrganizationSettingsRepository organizationSettingsRepository;
    private final OrganizationRepository organizationRepository;
    private final PartnerProfileRepository partnerProfileRepository;
    private final AuditComplianceService auditComplianceService;
    private final ObjectMapper objectMapper;

    @GetMapping
    @PreAuthorize(
            "hasAnyRole('PARTNER_ADMIN', 'CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR', 'UNDERWRITER', "
                    + "'INSURER_PRODUCT_MANAGER', 'FINANCE_OFFICER', 'CUSTOMER_SUPPORT', 'PLATFORM_ADMIN')")
    @Operation(summary = "Get organization settings JSON")
    public ApiResponse<OrganizationSettingsResponse> getSettings() {
        UUID orgId = requireOrganizationId();
        OrganizationSettings settings = requireSettings(orgId);
        Organization organization =
                organizationRepository.findById(orgId).orElseThrow(() -> new BusinessException("Organization not found"));
        PartnerProfile profile = partnerProfileRepository.findByOrganizationId(orgId).orElse(null);
        return ApiResponse.ok("Settings retrieved", toResponse(settings, organization, profile));
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Update organization settings JSON")
    public ApiResponse<OrganizationSettingsResponse> updateSettings(
            @Valid @RequestBody UpdateOrganizationSettingsRequest request) {
        UUID orgId = requireOrganizationId();
        OrganizationSettings settings = requireSettings(orgId);
        settings.setSettingsJson(request.getSettingsJson());
        settings.setUpdatedAt(Instant.now());
        organizationSettingsRepository.save(settings);

        syncPartnerProfile(orgId, request.getSettingsJson());

        auditComplianceService.log(
                "ORG_SETTINGS_UPDATED",
                "ORGANIZATION_SETTINGS",
                settings.getOrganizationId(),
                "Organization settings updated");
        Organization organization = organizationRepository
                .findById(orgId)
                .orElseThrow(() -> new BusinessException("Organization not found"));
        PartnerProfile profile = partnerProfileRepository.findByOrganizationId(orgId).orElse(null);
        return ApiResponse.ok("Settings updated", toResponse(settings, organization, profile));
    }

    private void syncPartnerProfile(UUID orgId, String settingsJson) {
        if (!StringUtils.hasText(settingsJson)) {
            return;
        }
        try {
            Map<String, Object> parsed =
                    objectMapper.readValue(settingsJson, new TypeReference<Map<String, Object>>() {});
            PartnerProfile profile = partnerProfileRepository
                    .findByOrganizationId(orgId)
                    .orElse(null);
            if (profile == null) {
                return;
            }
            if (parsed.get("contactEmail") != null) {
                profile.setContactEmail(String.valueOf(parsed.get("contactEmail")));
            }
            if (parsed.get("contactPhone") != null) {
                profile.setContactPhone(String.valueOf(parsed.get("contactPhone")));
            }
            if (parsed.get("registrationNumber") != null) {
                profile.setRegistrationNumber(String.valueOf(parsed.get("registrationNumber")));
            }
            if (parsed.get("addressLine") != null) {
                profile.setAddressLine(String.valueOf(parsed.get("addressLine")));
            }
            if (parsed.get("district") != null) {
                profile.setDistrict(String.valueOf(parsed.get("district")));
            }
            if (parsed.get("website") != null) {
                profile.setWebsite(String.valueOf(parsed.get("website")));
            }
            profile.setUpdatedAt(Instant.now());
            partnerProfileRepository.save(profile);
        } catch (Exception ignored) {
            // settingsJson may be partial or malformed — org settings still saved
        }
    }

    private UUID requireOrganizationId() {
        UUID orgId = SecurityUtils.currentUser().getOrganizationId();
        if (orgId == null) {
            throw new BusinessException("No organization associated with this account");
        }
        return orgId;
    }

    private OrganizationSettings requireSettings(UUID orgId) {
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

    private OrganizationSettingsResponse toResponse(
            OrganizationSettings settings, Organization organization, PartnerProfile profile) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (StringUtils.hasText(settings.getSettingsJson())) {
            try {
                merged.putAll(objectMapper.readValue(settings.getSettingsJson(), new TypeReference<>() {}));
            } catch (Exception ignored) {
                merged.put("settingsJson", settings.getSettingsJson());
            }
        }
        merged.putIfAbsent("name", organization.getName());
        if (profile != null) {
            merged.putIfAbsent("contactEmail", profile.getContactEmail());
            merged.putIfAbsent("contactPhone", profile.getContactPhone());
            merged.putIfAbsent("registrationNumber", profile.getRegistrationNumber());
            merged.putIfAbsent("addressLine", profile.getAddressLine());
            merged.putIfAbsent("district", profile.getDistrict());
            merged.putIfAbsent("website", profile.getWebsite());
            merged.putIfAbsent("country", profile.getCountry());
        }
        String enrichedJson;
        try {
            enrichedJson = objectMapper.writeValueAsString(merged);
        } catch (Exception ex) {
            enrichedJson = settings.getSettingsJson();
        }
        return OrganizationSettingsResponse.builder()
                .organizationId(settings.getOrganizationId())
                .organizationName(organization.getName())
                .settingsJson(enrichedJson)
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
