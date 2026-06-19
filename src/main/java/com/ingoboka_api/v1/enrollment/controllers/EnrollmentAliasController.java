package com.ingoboka_api.v1.enrollment.controllers;

import com.ingoboka_api.v1.common.requests.QuickApplicationRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.ApplicationResponse;
import com.ingoboka_api.v1.enrollment.services.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Tag(name = "Enrollment aliases", description = "Blueprint alias for one-shot enrollment")
@SecurityRequirement(name = "bearerAuth")
public class EnrollmentAliasController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Create enrollment", description = "Alias: create + start application in one call")
    public ApiResponse<ApplicationResponse> enroll(@Valid @RequestBody QuickApplicationRequest request) {
        return ApiResponse.ok("Enrollment created", enrollmentService.createQuickApplication(request));
    }
}
