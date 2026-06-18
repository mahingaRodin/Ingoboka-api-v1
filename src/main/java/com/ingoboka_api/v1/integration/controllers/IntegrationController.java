package com.ingoboka_api.v1.integration.controllers;

import com.ingoboka_api.v1.common.requests.InvokeIntegrationRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.IntegrationAdapterResponse;
import com.ingoboka_api.v1.common.responses.IntegrationInvokeResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.integration.services.IntegrationAdapterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations")
@RequiredArgsConstructor
@Tag(name = "Integration Adapters", description = "Payment, messaging, identity, and insurer-core connectors")
@SecurityRequirement(name = "bearerAuth")
public class IntegrationController {

    private final IntegrationAdapterService integrationAdapterService;

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PARTNER_ADMIN')")
    @Operation(summary = "List integration adapters (paginated)")
    public ApiResponse<PageResponse<IntegrationAdapterResponse>> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Adapters retrieved", integrationAdapterService.listAdapters(page, size));
    }

    @PostMapping("/{code}/invoke")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PARTNER_ADMIN')")
    @Operation(summary = "Invoke sandbox integration adapter")
    public ApiResponse<IntegrationInvokeResponse> invoke(
            @PathVariable String code, @RequestBody(required = false) InvokeIntegrationRequest request) {
        InvokeIntegrationRequest body = request != null ? request : new InvokeIntegrationRequest();
        return ApiResponse.ok("Adapter invoked", integrationAdapterService.invoke(code, body));
    }
}
