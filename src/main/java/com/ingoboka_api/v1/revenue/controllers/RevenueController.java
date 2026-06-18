package com.ingoboka_api.v1.revenue.controllers;

import com.ingoboka_api.v1.common.requests.CreateContractPriceRuleRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.ContractPriceRuleResponse;
import com.ingoboka_api.v1.common.responses.InvoiceResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.RevenueLedgerResponse;
import com.ingoboka_api.v1.revenue.services.RevenueCommissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/revenue", "/api/v1/revenue", "/api/v1/insurer/revenue"})
@RequiredArgsConstructor
@Tag(name = "Revenue & Commission", description = "Pricing rules, commission ledger, and invoices")
@SecurityRequirement(name = "bearerAuth")
public class RevenueController {

    private final RevenueCommissionService revenueCommissionService;

    @PostMapping("/price-rules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'FINANCE_OFFICER', 'PLATFORM_ADMIN')")
    @Operation(summary = "Create contract price rule")
    public ApiResponse<ContractPriceRuleResponse> createRule(@Valid @RequestBody CreateContractPriceRuleRequest request) {
        return ApiResponse.ok("Price rule created", revenueCommissionService.createPriceRule(request));
    }

    @GetMapping("/price-rules")
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'FINANCE_OFFICER', 'PLATFORM_ADMIN')")
    @Operation(summary = "List price rules (paginated)")
    public ApiResponse<PageResponse<ContractPriceRuleResponse>> listRules(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Price rules retrieved", revenueCommissionService.listPriceRules(page, size));
    }

    @GetMapping("/ledger")
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'FINANCE_OFFICER', 'PLATFORM_ADMIN')")
    @Operation(summary = "List revenue ledger entries (paginated)")
    public ApiResponse<PageResponse<RevenueLedgerResponse>> listLedger(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Ledger retrieved", revenueCommissionService.listLedger(page, size));
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'FINANCE_OFFICER', 'PLATFORM_ADMIN')")
    @Operation(summary = "List invoices (paginated)")
    public ApiResponse<PageResponse<InvoiceResponse>> listInvoices(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Invoices retrieved", revenueCommissionService.listInvoices(page, size));
    }

    @PostMapping("/invoices/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('PARTNER_ADMIN', 'FINANCE_OFFICER', 'PLATFORM_ADMIN')")
    @Operation(summary = "Generate commission invoice for period")
    public ApiResponse<InvoiceResponse> generateInvoice(
            @RequestParam java.time.LocalDate periodStart, @RequestParam java.time.LocalDate periodEnd) {
        return ApiResponse.ok(
                "Invoice generated", revenueCommissionService.generateInvoice(periodStart, periodEnd));
    }
}
