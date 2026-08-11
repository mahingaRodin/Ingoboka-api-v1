package com.ingoboka_api.v1.customer.controllers;

import com.ingoboka_api.v1.common.enums.ConsentType;
import com.ingoboka_api.v1.common.requests.CreateDependantRequest;
import com.ingoboka_api.v1.common.requests.GrantConsentRequest;
import com.ingoboka_api.v1.common.requests.SaveNeedsAssessmentPreferencesRequest;
import com.ingoboka_api.v1.common.requests.UpdateCitizenAccountRequest;
import com.ingoboka_api.v1.common.requests.UpdateCitizenProfileRequest;
import com.ingoboka_api.v1.common.requests.UpdateDependantRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.CitizenAccountResponse;
import com.ingoboka_api.v1.common.responses.CitizenProfileResponse;
import com.ingoboka_api.v1.common.responses.ConsentResponse;
import com.ingoboka_api.v1.common.responses.DependantResponse;
import com.ingoboka_api.v1.common.responses.NeedsAssessmentPreferencesResponse;
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
@RequestMapping("/api/v1/customers/me")
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

    @PutMapping("/account")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Update my account email", description = "Changing email requires re-verification before portal access is restored")
    public ApiResponse<CitizenAccountResponse> updateMyAccount(@Valid @RequestBody UpdateCitizenAccountRequest request) {
        CitizenAccountResponse account = customerProfileService.updateMyAccount(request);
        String message = account.isRequiresEmailVerification()
                ? "Email updated — verify your new address to restore access"
                : "Account updated";
        return ApiResponse.ok(message, account);
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

    @PutMapping("/dependants/{dependantId}")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Update dependant")
    public ApiResponse<DependantResponse> updateDependant(
            @PathVariable UUID dependantId, @Valid @RequestBody UpdateDependantRequest request) {
        return ApiResponse.ok("Dependant updated", customerProfileService.updateDependant(dependantId, request));
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

    @GetMapping("/preferences/needs-assessment")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Get saved needs assessment preferences")
    public ApiResponse<NeedsAssessmentPreferencesResponse> getNeedsAssessmentPreferences() {
        return ApiResponse.ok(
                "Needs assessment preferences", customerProfileService.getNeedsAssessmentPreferences());
    }

    @PostMapping("/preferences/needs-assessment")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Save needs assessment preferences")
    public ApiResponse<NeedsAssessmentPreferencesResponse> saveNeedsAssessmentPreferences(
            @Valid @RequestBody SaveNeedsAssessmentPreferencesRequest request) {
        return ApiResponse.ok(
                "Needs assessment saved", customerProfileService.saveNeedsAssessmentPreferences(request));
    }
}
