package com.ingoboka_api.v1.billing.controllers;

import com.ingoboka_api.v1.billing.services.BillingFinanceService;
import com.ingoboka_api.v1.common.requests.CreateRefundRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.BillResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.ReceiptResponse;
import com.ingoboka_api.v1.common.responses.ReconciliationResponse;
import com.ingoboka_api.v1.common.responses.RefundResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
@Tag(name = "Billing Finance", description = "Bills, receipts, refunds, and reconciliation")
@SecurityRequirement(name = "bearerAuth")
public class BillingFinanceController {

    private final BillingFinanceService billingFinanceService;

    @PostMapping("/refunds")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'PLATFORM_ADMIN', 'PARTNER_ADMIN')")
    @Operation(summary = "Create refund for a successful payment")
    public ApiResponse<RefundResponse> createRefund(@Valid @RequestBody CreateRefundRequest request) {
        return ApiResponse.ok("Refund created", billingFinanceService.createRefund(request));
    }

    @GetMapping("/refunds/payment/{paymentId}")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'PLATFORM_ADMIN', 'PARTNER_ADMIN', 'CITIZEN')")
    @Operation(summary = "List refunds for payment")
    public ApiResponse<PageResponse<RefundResponse>> listRefunds(
            @PathVariable UUID paymentId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Refunds retrieved", billingFinanceService.listRefunds(paymentId, page, size));
    }

    @GetMapping("/bills/policy/{policyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List bills for policy")
    public ApiResponse<PageResponse<BillResponse>> listBills(
            @PathVariable UUID policyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Bills retrieved", billingFinanceService.listPolicyBills(policyId, page, size));
    }

    @GetMapping("/receipts/policy/{policyId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List receipts for policy")
    public ApiResponse<PageResponse<ReceiptResponse>> listReceipts(
            @PathVariable UUID policyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Receipts retrieved", billingFinanceService.listPolicyReceipts(policyId, page, size));
    }

    @PostMapping("/reconciliation")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'PLATFORM_ADMIN', 'PARTNER_ADMIN')")
    @Operation(summary = "Run daily reconciliation for tenant")
    public ApiResponse<ReconciliationResponse> reconcile(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok("Reconciliation completed", billingFinanceService.runReconciliation(date));
    }

    @GetMapping("/reconciliation")
    @PreAuthorize("hasAnyRole('FINANCE_OFFICER', 'PLATFORM_ADMIN', 'PARTNER_ADMIN')")
    @Operation(summary = "List reconciliation records")
    public ApiResponse<PageResponse<ReconciliationResponse>> listReconciliations(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(
                "Reconciliation records retrieved", billingFinanceService.listReconciliations(page, size));
    }
}
