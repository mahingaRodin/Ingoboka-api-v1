package com.ingoboka_api.v1.frontend.controllers;

import com.ingoboka_api.v1.billing.services.BillingService;
import com.ingoboka_api.v1.claim.services.ClaimService;
import com.ingoboka_api.v1.common.enums.ApplicationStatus;
import com.ingoboka_api.v1.common.enums.ClaimStatus;
import com.ingoboka_api.v1.common.requests.*;
import com.ingoboka_api.v1.common.responses.*;
import com.ingoboka_api.v1.customer.services.CustomerProfileService;
import com.ingoboka_api.v1.enrollment.services.EnrollmentService;
import com.ingoboka_api.v1.claim.repositories.ClaimRepository;
import com.ingoboka_api.v1.enrollment.repositories.PolicyApplicationRepository;
import com.ingoboka_api.v1.identity.repositories.OrganizationRepository;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import com.ingoboka_api.v1.policy.services.PolicyService;
import com.ingoboka_api.v1.reporting.services.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Frontend compatibility", description = "Aliases expected by Next.js MVP clients")
public class FrontendCompatController {

    private final BillingService billingService;
    private final PolicyService policyService;
    private final ClaimService claimService;
    private final EnrollmentService enrollmentService;
    private final CustomerProfileService customerProfileService;
    private final ReportingService reportingService;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final PolicyRepository policyRepository;
    private final PolicyApplicationRepository policyApplicationRepository;
    private final ClaimRepository claimRepository;

    @GetMapping("/api/v1/customers/me")
    @PreAuthorize("hasRole('CITIZEN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<CitizenProfileResponse> customersMe() {
        return ApiResponse.ok("Profile retrieved", customerProfileService.getMyProfile());
    }

    @PostMapping("/api/v1/customers/consent")
    @PreAuthorize("hasRole('CITIZEN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ConsentResponse> customersConsent(
            @Valid @RequestBody FrontendConsentRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(
                "Consent recorded", customerProfileService.grantFrontendConsent(request, httpRequest.getRemoteAddr()));
    }

    @PostMapping("/api/v1/customer/kyc/submit")
    @PreAuthorize("hasRole('CITIZEN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<CitizenProfileResponse> submitKyc() {
        return ApiResponse.ok("KYC submitted", customerProfileService.submitKyc());
    }

    @GetMapping("/api/v1/customer/dependants")
    @PreAuthorize("hasRole('CITIZEN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PageResponse<DependantResponse>> customerDependants(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Dependants retrieved", customerProfileService.listMyDependants(page, size));
    }

    @PostMapping("/api/v1/customer/dependants")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CITIZEN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<DependantResponse> addCustomerDependant(@Valid @RequestBody CreateDependantRequest request) {
        return ApiResponse.ok("Dependant added", customerProfileService.addDependant(request));
    }

    @DeleteMapping("/api/v1/customer/dependants/{dependantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('CITIZEN')")
    @SecurityRequirement(name = "bearerAuth")
    public void removeCustomerDependant(@PathVariable UUID dependantId) {
        customerProfileService.removeDependant(dependantId);
    }

    @PostMapping("/api/v1/payments/initiate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CITIZEN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PaymentResponse> initiatePaymentAlias(@Valid @RequestBody InitiatePaymentRequest request) {
        return ApiResponse.ok("Payment initiated", billingService.initiatePayment(request));
    }

    @PostMapping("/api/v1/payments/sandbox/callback")
    @Operation(summary = "Sandbox payment callback for frontend demo")
    public ApiResponse<PaymentResponse> sandboxCallback(@Valid @RequestBody SandboxPaymentCallbackRequest request) {
        PaymentWebhookRequest mapped = new PaymentWebhookRequest();
        mapped.setProviderReference(request.getProviderReference());
        mapped.setStatus(request.getStatus());
        mapped.setIdempotencyKey(
                request.getIdempotencyKey() != null ? request.getIdempotencyKey() : UUID.randomUUID().toString());
        return ApiResponse.ok("Webhook processed", billingService.processWebhook("SANDBOX", mapped));
    }

    @GetMapping("/api/v1/payments/{paymentId}/status")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PaymentStatusResponse> paymentStatus(@PathVariable UUID paymentId) {
        return ApiResponse.ok("Payment status", billingService.getPaymentStatus(paymentId));
    }

    @GetMapping("/api/v1/policies")
    @PreAuthorize("hasRole('CITIZEN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PageResponse<PolicyResponse>> policiesAlias(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Policies retrieved", policyService.listMyPolicies(page, size));
    }

    @GetMapping("/api/v1/policies/{policyId}/card")
    @PreAuthorize("hasAnyRole('CITIZEN', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PolicyCardResponse> policyCard(@PathVariable UUID policyId) {
        return ApiResponse.ok("Policy card", policyService.getPolicyCard(policyId));
    }

    @GetMapping("/api/v1/verify/{token}")
    public ApiResponse<PolicyVerificationResponse> verifyAlias(@PathVariable String token) {
        return ApiResponse.ok("Policy verification result", policyService.verifyByQrToken(token));
    }

    @GetMapping("/api/v1/admin/claims")
    @PreAuthorize("hasAnyRole('CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PageResponse<ClaimResponse>> adminClaims(
            @RequestParam(required = false) ClaimStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Claims retrieved", claimService.listTenantClaims(status, page, size));
    }

    @GetMapping("/api/v1/admin/claims/{claimId}")
    @PreAuthorize("hasAnyRole('CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ClaimResponse> adminClaim(@PathVariable UUID claimId) {
        return ApiResponse.ok("Claim retrieved", claimService.getClaim(claimId));
    }

    @PostMapping("/api/v1/admin/claims/{claimId}/decision")
    @PreAuthorize("hasAnyRole('CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR', 'PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ClaimResponse> adminClaimDecision(
            @PathVariable UUID claimId, @Valid @RequestBody RecordClaimDecisionRequest request) {
        return ApiResponse.ok("Decision recorded", claimService.recordDecision(claimId, request));
    }

    @GetMapping("/api/v1/admin/reports/overview")
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'CLAIMS_OFFICER', 'PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Map<String, Object>> adminReportsOverview() {
        TenantOverviewResponse overview = reportingService.getTenantOverview();
        PolicyReportSummaryResponse policySummary = reportingService.getPolicyReportSummary();
        return ApiResponse.ok(
                "Overview",
                Map.of(
                        "openClaims", overview.getOpenClaims(),
                        "activePolicies", overview.getActivePolicies(),
                        "pendingApplications", overview.getPendingApplications(),
                        "successfulPayments", overview.getSuccessfulPayments(),
                        "citizensEnrolled", policySummary.getCitizensEnrolled()));
    }

    @GetMapping("/api/v1/admin/reports/claims-breakdown")
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'CLAIMS_OFFICER', 'PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ClaimsBreakdownResponse> adminClaimsBreakdown() {
        return ApiResponse.ok("Claims breakdown", claimService.getClaimsBreakdown());
    }

    @GetMapping("/api/v1/admin/platform/overview")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Map<String, Object>> platformOverview() {
        long openClaims = claimRepository.findAll().stream()
                .filter(claim -> claim.getStatus() == ClaimStatus.SUBMITTED
                        || claim.getStatus() == ClaimStatus.UNDER_REVIEW)
                .count();
        return ApiResponse.ok(
                "Platform overview",
                Map.of(
                        "organizations", organizationRepository.count(),
                        "activeUsers", userRepository.count(),
                        "activePolicies", policyRepository.count(),
                        "openClaims", openClaims,
                        "totalApplications", policyApplicationRepository.count()));
    }

    @GetMapping("/api/v1/agent/applications")
    @PreAuthorize("hasRole('AGENT')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PageResponse<ApplicationResponse>> agentApplications(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Applications", enrollmentService.listAgentApplications(page, size));
    }

    @PostMapping("/api/v1/agent/applications")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('AGENT')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ApplicationResponse> agentCreateApplication(
            @Valid @RequestBody AgentAssistedApplicationRequest request) {
        return ApiResponse.ok(
                "Assisted application created",
                enrollmentService.createAgentAssistedApplication(request.getCitizenPhone(), request.getProductPlanId()));
    }
}
