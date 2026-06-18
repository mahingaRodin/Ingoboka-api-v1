package com.ingoboka_api.v1.billing.controllers;

import com.ingoboka_api.v1.billing.services.BillingService;
import com.ingoboka_api.v1.common.requests.InitiatePaymentRequest;
import com.ingoboka_api.v1.common.requests.MomoPaymentWebhookRequest;
import com.ingoboka_api.v1.common.requests.PaymentWebhookRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
@RequestMapping({"/api/payments", "/api/v1/payments"})
@RequiredArgsConstructor
@Tag(name = "Billing & Payment", description = "Premium payment initiation and sandbox provider callbacks")
public class BillingController {

    private final BillingService billingService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "Initiate payment", description = "Start sandbox payment for a policy awaiting premium")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PaymentResponse> initiatePayment(@Valid @RequestBody InitiatePaymentRequest request) {
        return ApiResponse.ok("Payment initiated", billingService.initiatePayment(request));
    }

    @PostMapping("/webhooks/{provider}")
    @Operation(summary = "Payment provider webhook", description = "Idempotent callback processing for sandbox/real providers")
    public ApiResponse<PaymentResponse> paymentWebhook(
            @PathVariable String provider, @Valid @RequestBody PaymentWebhookRequest request) {
        return ApiResponse.ok("Webhook processed", billingService.processWebhook(provider, request));
    }

    @PostMapping("/webhooks/momo")
    @Operation(
            summary = "MTN MoMo sandbox webhook",
            description = "MoMo-shaped callback: externalId, status SUCCESSFUL/FAILED, financialTransactionId")
    public ApiResponse<PaymentResponse> momoWebhook(@Valid @RequestBody MomoPaymentWebhookRequest request) {
        return ApiResponse.ok("MoMo webhook processed", billingService.processMomoWebhook(request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CITIZEN')")
    @Operation(summary = "List my payments")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PageResponse<PaymentResponse>> listMyPayments(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Payments retrieved", billingService.listMyPayments(page, size));
    }

    @GetMapping("/policy/{policyId}")
    @PreAuthorize("hasAnyRole('CITIZEN', 'FINANCE_OFFICER', 'PARTNER_ADMIN', 'PLATFORM_ADMIN')")
    @Operation(summary = "List payments for policy")
    @SecurityRequirement(name = "bearerAuth")
    public ApiResponse<PageResponse<PaymentResponse>> listPolicyPayments(
            @PathVariable UUID policyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("Payments retrieved", billingService.listPolicyPayments(policyId, page, size));
    }
}
