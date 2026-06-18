package com.ingoboka_api.v1.partner.controllers;

import com.ingoboka_api.v1.common.requests.CreatePartnerContractRequest;
import com.ingoboka_api.v1.common.requests.UpdateContractStatusRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.PartnerContractResponse;
import com.ingoboka_api.v1.partner.services.PartnerContractService;
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
@RequestMapping("/api/v1/partners/{partnerId}/contracts")
@RequiredArgsConstructor
@Tag(name = "Partner Contracts", description = "B2B partner pricing and contract management")
@SecurityRequirement(name = "bearerAuth")
public class PartnerContractController {

    private final PartnerContractService partnerContractService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Create contract", description = "Platform admin creates a partner commercial contract")
    public ApiResponse<PartnerContractResponse> createContract(
            @PathVariable UUID partnerId, @Valid @RequestBody CreatePartnerContractRequest request) {
        return ApiResponse.ok("Contract created", partnerContractService.createContract(partnerId, request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'PARTNER_ADMIN')")
    @Operation(summary = "List contracts", description = "View contracts for a partner tenant")
    public ApiResponse<PageResponse<PartnerContractResponse>> listContracts(
            @PathVariable UUID partnerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Contracts retrieved", partnerContractService.listContracts(partnerId, page, size));
    }

    @PatchMapping("/{contractId}/status")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(summary = "Update contract status", description = "Activate, suspend, or terminate a contract")
    public ApiResponse<PartnerContractResponse> updateContractStatus(
            @PathVariable UUID partnerId,
            @PathVariable UUID contractId,
            @Valid @RequestBody UpdateContractStatusRequest request) {
        return ApiResponse.ok(
                "Contract status updated",
                partnerContractService.updateContractStatus(partnerId, contractId, request));
    }
}
