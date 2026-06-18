package com.ingoboka_api.v1.agent.controllers;

import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.enrollment.repositories.PolicyApplicationRepository;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent/dashboard")
@RequiredArgsConstructor
@Tag(name = "Agent Portal", description = "Field officer assisted enrollment dashboard")
@SecurityRequirement(name = "bearerAuth")
public class AgentDashboardController {

    private final PolicyApplicationRepository policyApplicationRepository;
    private final PolicyRepository policyRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'PLATFORM_ADMIN', 'PARTNER_ADMIN')")
    @Operation(summary = "Agent operational dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(
                "Agent dashboard retrieved",
                Map.of(
                        "totalApplications", policyApplicationRepository.count(),
                        "totalPolicies", policyRepository.count()));
    }
}
