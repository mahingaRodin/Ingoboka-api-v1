package com.ingoboka_api.v1.partner.controllers;

import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.CreateStaffRequest;
import com.ingoboka_api.v1.common.requests.ResetManagedUserPasswordRequest;
import com.ingoboka_api.v1.common.requests.UpdateStaffRequest;
import com.ingoboka_api.v1.common.requests.UpdateStaffStatusRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.PartnerStaffOverviewResponse;
import com.ingoboka_api.v1.common.responses.StaffCreatedResponse;
import com.ingoboka_api.v1.common.responses.StaffResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.partner.services.PartnerStaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Convenience routes for PARTNER_ADMIN — resolves organization from the logged-in user
 * so the frontend does not need to pass partnerId on every staff-management call.
 */
@RestController
@RequestMapping("/api/v1/partner")
@RequiredArgsConstructor
@Tag(name = "Partner portal", description = "Partner admin staff management and oversight")
@SecurityRequirement(name = "bearerAuth")
public class PartnerPortalController {

    private final PartnerStaffService partnerStaffService;

    @GetMapping("/staff/overview")
    @PreAuthorize("hasRole('PARTNER_ADMIN')")
    @Operation(summary = "Staff overview", description = "List all staff with onboarding status counts for partner admin dashboard")
    public ApiResponse<PartnerStaffOverviewResponse> staffOverview() {
        return ApiResponse.ok("Staff overview", partnerStaffService.getStaffOverview(requirePartnerOrganizationId()));
    }

    @GetMapping("/staff")
    @PreAuthorize("hasRole('PARTNER_ADMIN')")
    @Operation(summary = "List my organization staff")
    public ApiResponse<PageResponse<StaffResponse>> listStaff(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(
                "Staff retrieved", partnerStaffService.listStaff(requirePartnerOrganizationId(), page, size));
    }

    @GetMapping("/staff/{userId}")
    @PreAuthorize("hasRole('PARTNER_ADMIN')")
    @Operation(summary = "Get staff member in my organization")
    public ApiResponse<StaffResponse> getStaff(@PathVariable UUID userId) {
        return ApiResponse.ok("Staff retrieved", partnerStaffService.getStaff(requirePartnerOrganizationId(), userId));
    }

    @PostMapping("/staff")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PARTNER_ADMIN')")
    @Operation(
            summary = "Create staff in my organization",
            description = "Same flow as partner onboarding: temporary password emailed, must change before use")
    public ApiResponse<StaffCreatedResponse> createStaff(@Valid @RequestBody CreateStaffRequest request) {
        return ApiResponse.ok(
                "Staff member created", partnerStaffService.createStaff(requirePartnerOrganizationId(), request));
    }

    @PutMapping("/staff/{userId}")
    @PreAuthorize("hasRole('PARTNER_ADMIN')")
    @Operation(summary = "Update staff in my organization")
    public ApiResponse<StaffResponse> updateStaff(
            @PathVariable UUID userId, @Valid @RequestBody UpdateStaffRequest request) {
        return ApiResponse.ok(
                "Staff updated", partnerStaffService.updateStaff(requirePartnerOrganizationId(), userId, request));
    }

    @PatchMapping("/staff/{userId}/status")
    @PreAuthorize("hasRole('PARTNER_ADMIN')")
    @Operation(summary = "Update staff status", description = "Activate, disable, or lock a staff account")
    public ApiResponse<StaffResponse> updateStaffStatus(
            @PathVariable UUID userId, @Valid @RequestBody UpdateStaffStatusRequest request) {
        return ApiResponse.ok(
                "Staff status updated",
                partnerStaffService.updateStaffStatus(requirePartnerOrganizationId(), userId, request));
    }

    @PostMapping("/staff/{userId}/reset-credentials")
    @PreAuthorize("hasRole('PARTNER_ADMIN')")
    @Operation(summary = "Reset staff credentials", description = "Email new temporary password; staff must change it on next login")
    public ApiResponse<StaffResponse> resetStaffCredentials(
            @PathVariable UUID userId, @RequestBody(required = false) ResetManagedUserPasswordRequest request) {
        return ApiResponse.ok(
                "Credentials reset email sent",
                partnerStaffService.resetStaffCredentials(
                        requirePartnerOrganizationId(),
                        userId,
                        request != null ? request : new ResetManagedUserPasswordRequest()));
    }

    @DeleteMapping("/staff/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PARTNER_ADMIN')")
    @Operation(summary = "Disable staff in my organization")
    public void deleteStaff(@PathVariable UUID userId) {
        partnerStaffService.deleteStaff(requirePartnerOrganizationId(), userId);
    }

    private UUID requirePartnerOrganizationId() {
        UUID organizationId = SecurityUtils.currentUser().getOrganizationId();
        if (organizationId == null) {
            throw new BusinessException("No organization associated with this partner admin account");
        }
        return organizationId;
    }
}
