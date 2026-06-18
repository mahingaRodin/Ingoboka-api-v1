package com.ingoboka_api.v1.customer.controllers;

import com.ingoboka_api.v1.common.enums.ConsentType;
import com.ingoboka_api.v1.common.requests.CreateDependantRequest;
import com.ingoboka_api.v1.common.requests.GrantConsentRequest;
import com.ingoboka_api.v1.common.requests.UpdateCitizenProfileRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.CitizenProfileResponse;
import com.ingoboka_api.v1.common.responses.ConsentResponse;
import com.ingoboka_api.v1.common.responses.DependantResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.customer.services.CustomerProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/customers/me", "/api/v1/customer/me", "/api/v1/customers/me"})
@RequiredArgsConstructor
@Tag(name = "Customer Profile & Consent", description = "Citizen profile, dependants, and legal consent management")
@SecurityRequirement(name = "bearerAuth")
public class CustomerProfileController {

    private final CustomerProfileService customerProfileService;

    @GetMapping("/profile")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Get my profile")
    public ApiResponse<CitizenProfileResponse> getMyProfile() {
        return ApiResponse.ok("Profile retrieved", customerProfileService.getMyProfile());
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Create or update my profile")
    public ApiResponse<CitizenProfileResponse> updateMyProfile(
            @Valid @RequestBody UpdateCitizenProfileRequest request) {
        return ApiResponse.ok("Profile saved", customerProfileService.updateMyProfile(request));
    }

    @GetMapping("/dependants")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "List my dependants")
    public ApiResponse<PageResponse<DependantResponse>> listDependants(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Dependants retrieved", customerProfileService.listMyDependants(page, size));
    }

    @PostMapping("/dependants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Add dependant")
    public ApiResponse<DependantResponse> addDependant(@Valid @RequestBody CreateDependantRequest request) {
        return ApiResponse.ok("Dependant added", customerProfileService.addDependant(request));
    }

    @DeleteMapping("/dependants/{dependantId}")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Remove dependant")
    public ApiResponse<Void> removeDependant(@PathVariable UUID dependantId) {
        customerProfileService.removeDependant(dependantId);
        return ApiResponse.ok("Dependant removed", null);
    }

    @PostMapping("/consents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Grant consent", description = "Records versioned consent evidence with optional IP address")
    public ApiResponse<ConsentResponse> grantConsent(
            @Valid @RequestBody GrantConsentRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        return ApiResponse.ok("Consent granted", customerProfileService.grantConsent(request, ip));
    }

    @GetMapping("/consents")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "List my consents")
    public ApiResponse<PageResponse<ConsentResponse>> listConsents(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Consents retrieved", customerProfileService.listMyConsents(page, size));
    }

    @DeleteMapping("/consents/{consentType}")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Revoke consent")
    public ApiResponse<Void> revokeConsent(@PathVariable ConsentType consentType) {
        customerProfileService.revokeConsent(consentType);
        return ApiResponse.ok("Consent revoked", null);
    }
}
