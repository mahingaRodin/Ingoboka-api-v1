package com.ingoboka_api.v1.partner.controllers;

import com.ingoboka_api.v1.common.requests.CreateStaffRequest;
import com.ingoboka_api.v1.common.requests.UpdateStaffStatusRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.StaffCreatedResponse;
import com.ingoboka_api.v1.common.responses.StaffResponse;
import com.ingoboka_api.v1.partner.services.PartnerStaffService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/partners/{partnerId}/staff")
@RequiredArgsConstructor
@Tag(name = "Partner Staff", description = "Staff membership management for partner tenants")
@SecurityRequirement(name = "bearerAuth")
public class PartnerStaffController {

    private final PartnerStaffService partnerStaffService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PARTNER_ADMIN')")
    @Operation(summary = "Create staff member", description = "Invite staff with tenant role; sends activation email")
    public ApiResponse<StaffCreatedResponse> createStaff(
            @PathVariable UUID partnerId, @Valid @RequestBody CreateStaffRequest request) {
        return ApiResponse.ok("Staff member created", partnerStaffService.createStaff(partnerId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PARTNER_ADMIN')")
    @Operation(summary = "List staff", description = "List staff members for a partner organization")
    public ApiResponse<PageResponse<StaffResponse>> listStaff(
            @PathVariable UUID partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Staff retrieved", partnerStaffService.listStaff(partnerId, page, size));
    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PARTNER_ADMIN')")
    @Operation(summary = "Update staff status", description = "Activate, disable, or lock a staff account")
    public ApiResponse<StaffResponse> updateStaffStatus(
            @PathVariable UUID partnerId,
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateStaffStatusRequest request) {
        return ApiResponse.ok(
                "Staff status updated", partnerStaffService.updateStaffStatus(partnerId, userId, request));
    }
}
