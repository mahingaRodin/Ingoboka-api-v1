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
import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import com.ingoboka_api.v1.document.services.DocumentStorageService;
import com.ingoboka_api.v1.common.util.HashUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;

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
    private final AuditComplianceService auditComplianceService;
    private final DocumentStorageService documentStorageService;

    @Value("${ingoboka.platform.name:Ingoboka Platform}")
    private String platformName;
    @Value("${ingoboka.platform.default-locale:en}")
    private String defaultLocale;
    @Value("${ingoboka.platform.maintenance-mode:false}")
    private boolean maintenanceMode;
    @Value("${ingoboka.platform.api-base-url:/api/v1}")
    private String apiBaseUrl;
    @Value("${ingoboka.platform.support-email:support@ingoboka.rw}")
    private String supportEmail;

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
            @PathVariable UUID claimId, @RequestBody Map<String, Object> payload) {
        String decisionStr = (String) payload.get("decision");
        String reason = (String) payload.get("reason");
        if ("REQUEST_INFO".equalsIgnoreCase(decisionStr)) {
             UpdateClaimStatusRequest updateReq = new UpdateClaimStatusRequest();
             updateReq.setStatus(ClaimStatus.INFORMATION_REQUIRED);
             updateReq.setReason(reason);
             return ApiResponse.ok("Claim status updated", claimService.updateStatus(claimId, updateReq));
        } else {
             RecordClaimDecisionRequest req = new RecordClaimDecisionRequest();
             if ("APPROVED".equalsIgnoreCase(decisionStr)) req.setDecision(com.ingoboka_api.v1.common.enums.ClaimDecisionType.APPROVED);
             else if ("REJECTED".equalsIgnoreCase(decisionStr)) req.setDecision(com.ingoboka_api.v1.common.enums.ClaimDecisionType.REJECTED);
             else if ("PARTIAL".equalsIgnoreCase(decisionStr)) req.setDecision(com.ingoboka_api.v1.common.enums.ClaimDecisionType.PARTIAL);
             req.setReason(reason);
             if (payload.containsKey("approvedAmount") && payload.get("approvedAmount") != null) {
                 req.setApprovedAmount(new java.math.BigDecimal(payload.get("approvedAmount").toString()));
             }
             return ApiResponse.ok("Decision recorded", claimService.recordDecision(claimId, req));
        }
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

    @GetMapping("/api/v1/admin/audit-logs")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PARTNER_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PageResponse<com.ingoboka_api.v1.common.responses.AuditLogResponse>> adminAuditLogs(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Audit logs retrieved", auditComplianceService.listAuditLogs(page, size));
    }

    @GetMapping("/api/v1/admin/platform/settings")
    public ApiResponse<PlatformSettingsResponse> platformSettings() {
        return ApiResponse.ok("Platform settings", PlatformSettingsResponse.builder()
                .platformName(platformName)
                .defaultLocale(defaultLocale)
                .maintenanceMode(maintenanceMode)
                .apiBaseUrl(apiBaseUrl)
                .supportEmail(supportEmail)
                .build());
    }

    @GetMapping("/api/v1/insurer/applications")
    @PreAuthorize("hasAnyRole('UNDERWRITER', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PageResponse<ApplicationResponse>> insurerApplications(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        ApplicationStatus appStatus = null;
        if ("PENDING".equalsIgnoreCase(status)) {
            appStatus = ApplicationStatus.SUBMITTED;
        } else if (status != null) {
            try { appStatus = ApplicationStatus.valueOf(status); } catch (Exception e) {}
        }
        return ApiResponse.ok("Applications retrieved", enrollmentService.listTenantApplications(appStatus, page, size));
    }

    @PostMapping("/api/v1/insurer/applications/{applicationId}/decision")
    @PreAuthorize("hasAnyRole('UNDERWRITER', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<ApplicationResponse> insurerApplicationDecision(
            @PathVariable UUID applicationId, @RequestBody Map<String, String> payload) {
        String decisionStr = payload.get("decision");
        ApplicationStatus status = ApplicationStatus.UNDER_REVIEW;
        if ("APPROVED".equalsIgnoreCase(decisionStr)) status = ApplicationStatus.APPROVED;
        else if ("REJECTED".equalsIgnoreCase(decisionStr)) status = ApplicationStatus.REJECTED;
        
        ReviewApplicationRequest reviewReq = new ReviewApplicationRequest();
        reviewReq.setStatus(status);
        reviewReq.setDecisionReason(payload.get("reason"));
        return ApiResponse.ok("Application reviewed", enrollmentService.reviewApplication(applicationId, reviewReq));
    }

    @PostMapping(value = "/api/v1/claims/{claimId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CITIZEN', 'CLAIMS_OFFICER', 'CLAIMS_SUPERVISOR')")
    @Operation(summary = "Upload claim documents (multipart)")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<Void> uploadClaimDocuments(
            @PathVariable UUID claimId, 
            @RequestPart("files") MultipartFile[] files) {
        for (MultipartFile file : files) {
            try {
                String objectKey = "claims/" + claimId + "/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
                String contentType = file.getContentType();
                long size = file.getSize();
                byte[] bytes = file.getBytes();
                String checksum = HashUtils.sha256(bytes);
                documentStorageService.upload(objectKey, new ByteArrayInputStream(bytes), size, contentType);
                
                AttachClaimDocumentRequest attachReq = new AttachClaimDocumentRequest();
                attachReq.setDocumentType("SUPPORTING_DOCUMENT");
                attachReq.setObjectKey(objectKey);
                attachReq.setMimeType(contentType);
                attachReq.setSizeBytes(size);
                attachReq.setChecksum(checksum != null ? checksum : "unknown");
                claimService.attachDocument(claimId, attachReq);
            } catch (Exception e) {
                throw new com.ingoboka_api.v1.common.exception.BusinessException("File upload failed: " + e.getMessage());
            }
        }
        return ApiResponse.ok("Documents uploaded", null);
    }
}
