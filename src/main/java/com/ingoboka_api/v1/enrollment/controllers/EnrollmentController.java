package com.ingoboka_api.v1.enrollment.controllers;

import com.ingoboka_api.v1.common.enums.ApplicationStatus;
import com.ingoboka_api.v1.common.requests.GenerateQuoteRequest;
import com.ingoboka_api.v1.common.requests.ReviewApplicationRequest;
import com.ingoboka_api.v1.common.requests.SubmitApplicationRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.ApplicationResponse;
import com.ingoboka_api.v1.common.responses.QuoteResponse;
import com.ingoboka_api.v1.enrollment.services.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/applications")
@RequiredArgsConstructor
@Tag(name = "Quotation & Enrollment", description = "Quote generation, application submission, and insurer review")
@SecurityRequirement(name = "bearerAuth")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/quote")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Generate quote", description = "Needs assessment and eligibility check against a published plan")
    public ApiResponse<QuoteResponse> generateQuote(@Valid @RequestBody GenerateQuoteRequest request) {
        return ApiResponse.ok("Quote generated", enrollmentService.generateQuote(request));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Submit application", description = "Convert an active quote into an enrollment application with consent reference")
    public ApiResponse<ApplicationResponse> submitApplication(@Valid @RequestBody SubmitApplicationRequest request) {
        return ApiResponse.ok("Application submitted", enrollmentService.submitApplication(request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "List my applications")
    public ApiResponse<List<ApplicationResponse>> listMyApplications() {
        return ApiResponse.ok("Applications retrieved", enrollmentService.listMyApplications());
    }

    @GetMapping("/{applicationId}")
    @PreAuthorize("hasAnyRole('CITIZEN', 'UNDERWRITER', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Get application")
    public ApiResponse<ApplicationResponse> getApplication(@PathVariable UUID applicationId) {
        return ApiResponse.ok("Application retrieved", enrollmentService.getApplication(applicationId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('UNDERWRITER', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "List tenant applications", description = "Underwriter work queue for the insurer tenant")
    public ApiResponse<List<ApplicationResponse>> listTenantApplications(
            @RequestParam(required = false) ApplicationStatus status) {
        return ApiResponse.ok("Applications retrieved", enrollmentService.listTenantApplications(status));
    }

    @PatchMapping("/{applicationId}/review")
    @PreAuthorize("hasAnyRole('UNDERWRITER', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "Review application", description = "Approve, reject, or move application under review")
    public ApiResponse<ApplicationResponse> reviewApplication(
            @PathVariable UUID applicationId, @Valid @RequestBody ReviewApplicationRequest request) {
        return ApiResponse.ok("Application reviewed", enrollmentService.reviewApplication(applicationId, request));
    }
}
