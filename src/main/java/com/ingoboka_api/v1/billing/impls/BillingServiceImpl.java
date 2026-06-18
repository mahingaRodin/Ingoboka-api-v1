package com.ingoboka_api.v1.billing.impls;

import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import com.ingoboka_api.v1.billing.models.Bill;
import com.ingoboka_api.v1.billing.models.Payment;
import com.ingoboka_api.v1.billing.models.PaymentEvent;
import com.ingoboka_api.v1.billing.models.PremiumSchedule;
import com.ingoboka_api.v1.billing.repositories.BillRepository;
import com.ingoboka_api.v1.billing.repositories.PaymentEventRepository;
import com.ingoboka_api.v1.billing.repositories.PaymentRepository;
import com.ingoboka_api.v1.billing.repositories.PremiumScheduleRepository;
import com.ingoboka_api.v1.billing.services.BillingFinanceService;
import com.ingoboka_api.v1.billing.services.BillingService;
import com.ingoboka_api.v1.common.enums.PaymentStatus;
import com.ingoboka_api.v1.common.enums.PremiumScheduleStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.InitiatePaymentRequest;
import com.ingoboka_api.v1.common.requests.MomoPaymentWebhookRequest;
import com.ingoboka_api.v1.common.requests.PaymentWebhookRequest;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.PaymentInitiationResult;
import com.ingoboka_api.v1.common.responses.PaymentResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.common.util.PaginationUtils;
import com.ingoboka_api.v1.customer.models.CitizenProfile;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.integration.payment.MomoSandboxPaymentAdapter;
import com.ingoboka_api.v1.integration.payment.PaymentProviderAdapter;
import com.ingoboka_api.v1.integration.payment.PaymentProviderRegistry;
import com.ingoboka_api.v1.integration.payment.SandboxPaymentAdapter;
import com.ingoboka_api.v1.integration.services.OutboxPublisherService;
import com.ingoboka_api.v1.messaging.services.NotificationTemplateService;
import com.ingoboka_api.v1.policy.models.Policy;
import com.ingoboka_api.v1.policy.services.PolicyIssuanceService;
import com.ingoboka_api.v1.policy.services.PolicyService;
import com.ingoboka_api.v1.revenue.services.RevenueCommissionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PremiumScheduleRepository premiumScheduleRepository;
    private final BillRepository billRepository;
    private final PolicyService policyService;
    private final PolicyIssuanceService policyIssuanceService;
    private final CitizenProfileRepository citizenProfileRepository;
    private final UserRepository userRepository;
    private final RevenueCommissionService revenueCommissionService;
    private final BillingFinanceService billingFinanceService;
    private final NotificationTemplateService notificationTemplateService;
    private final AuditComplianceService auditComplianceService;
    private final OutboxPublisherService outboxPublisherService;
    private final PaymentProviderRegistry paymentProviderRegistry;

    @Override
    @Transactional
    public PaymentResponse initiatePayment(InitiatePaymentRequest request) {
        CitizenProfile profile = requireMyProfile();
        Policy policy = policyService.requirePolicyForPayment(request.getPolicyId(), profile.getId());

        if (paymentRepository.existsByPolicyIdAndStatus(policy.getId(), PaymentStatus.PENDING)) {
            throw new BusinessException("A payment is already pending for this policy");
        }

        String idempotencyKey = request.getIdempotencyKey() != null
                ? request.getIdempotencyKey()
                : UUID.randomUUID().toString();

        paymentRepository.findByIdempotencyKey(idempotencyKey).ifPresent(existing -> {
            throw new BusinessException("Duplicate payment request");
        });

        BigDecimal payAmount = request.getAmount() != null ? request.getAmount() : policy.getPremiumAmount();
        if (payAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be positive");
        }

        Instant now = Instant.now();
        String providerCode = request.getProvider() != null ? request.getProvider() : SandboxPaymentAdapter.CODE;
        PaymentProviderAdapter adapter = paymentProviderRegistry.require(providerCode);

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrganizationId(policy.getOrganizationId());
        payment.setPolicyId(policy.getId());
        payment.setCitizenProfileId(profile.getId());
        payment.setAmount(payAmount);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setProvider(providerCode);
        payment.setIdempotencyKey(idempotencyKey);
        payment.setInitiatedAt(now);

        PaymentInitiationResult initiation =
                adapter.initiate(payment, request, resolveUserPhone(profile));
        payment.setProviderReference(initiation.getProviderReference());
        paymentRepository.save(payment);

        return toResponse(payment, initiation);
    }

    @Override
    @Transactional
    public PaymentResponse processMomoWebhook(MomoPaymentWebhookRequest request) {
        String idempotencyKey = StringUtils.hasText(request.getIdempotencyKey())
                ? request.getIdempotencyKey()
                : request.getFinancialTransactionId();
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new BusinessException("idempotencyKey or financialTransactionId is required");
        }

        String normalizedStatus = normalizeMomoStatus(request.getStatus());
        PaymentWebhookRequest mapped = new PaymentWebhookRequest();
        mapped.setProviderReference(request.getExternalId());
        mapped.setStatus(normalizedStatus);
        mapped.setIdempotencyKey(idempotencyKey);
        return processWebhook(MomoSandboxPaymentAdapter.CODE, mapped);
    }

    private String normalizeMomoStatus(String status) {
        if ("SUCCESSFUL".equalsIgnoreCase(status) || "SUCCESS".equalsIgnoreCase(status)) {
            return "SUCCESS";
        }
        if ("FAILED".equalsIgnoreCase(status)) {
            return "FAILED";
        }
        throw new BusinessException("Unsupported MoMo payment status: " + status);
    }

    private String resolveUserPhone(CitizenProfile profile) {
        return userRepository
                .findById(profile.getUserId())
                .map(com.ingoboka_api.v1.identity.models.User::getPhoneNumber)
                .orElse(null);
    }

    @Override
    @Transactional
    public PaymentResponse processWebhook(String provider, PaymentWebhookRequest request) {
        if (paymentEventRepository.findByIdempotencyKey(request.getIdempotencyKey()).isPresent()) {
            Payment payment = paymentRepository
                    .findByProviderReference(request.getProviderReference())
                    .orElseThrow(() -> new BusinessException("Payment not found"));
            return toResponse(payment);
        }

        Payment payment = paymentRepository
                .findByProviderReference(request.getProviderReference())
                .orElseThrow(() -> new BusinessException("Payment not found"));

        Instant now = Instant.now();
        PaymentEvent event = new PaymentEvent();
        event.setId(UUID.randomUUID());
        event.setPaymentId(payment.getId());
        event.setEventType("WEBHOOK_" + request.getStatus().toUpperCase());
        event.setPayload("provider=" + provider);
        event.setIdempotencyKey(request.getIdempotencyKey());
        event.setCreatedAt(now);
        paymentEventRepository.save(event);

        if ("SUCCESS".equalsIgnoreCase(request.getStatus())) {
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setCompletedAt(now);
            paymentRepository.save(payment);
            markPremiumSchedulePaid(payment, now);
            policyIssuanceService.activatePolicy(payment.getPolicyId());
            onPaymentSuccess(payment);
        } else if ("FAILED".equalsIgnoreCase(request.getStatus())) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setCompletedAt(now);
            paymentRepository.save(payment);
        } else {
            throw new BusinessException("Unsupported payment status");
        }

        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> listMyPayments(int page, int size) {
        CitizenProfile profile = requireMyProfile();
        Page<Payment> result = paymentRepository.findByCitizenProfileIdOrderByInitiatedAtDesc(
                profile.getId(), PaginationUtils.toPageable(page, size, "initiatedAt"));
        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> listPolicyPayments(UUID policyId, int page, int size) {
        policyService.getPolicy(policyId);
        Page<Payment> result = paymentRepository.findByPolicyIdOrderByInitiatedAtDesc(
                policyId, PaginationUtils.toPageable(page, size, "initiatedAt"));
        return PageResponse.from(result.map(this::toResponse));
    }

    private void markPremiumSchedulePaid(Payment payment, Instant now) {
        premiumScheduleRepository
                .findFirstByPolicyIdAndStatusOrderByDueDateAsc(
                        payment.getPolicyId(), PremiumScheduleStatus.PENDING)
                .ifPresent(schedule -> {
                    schedule.setStatus(PremiumScheduleStatus.PAID);
                    schedule.setPaidAt(now);
                    schedule.setPaymentId(payment.getId());
                    premiumScheduleRepository.save(schedule);
                });
    }

    private void onPaymentSuccess(Payment payment) {
        PremiumSchedule schedule = premiumScheduleRepository
                .findFirstByPolicyIdAndStatusOrderByDueDateAsc(payment.getPolicyId(), PremiumScheduleStatus.PENDING)
                .orElse(null);
        Bill bill = schedule != null
                ? billRepository.findFirstByPremiumScheduleId(schedule.getId()).orElse(null)
                : null;
        boolean partial = bill != null && payment.getAmount().compareTo(bill.getAmount()) < 0;

        revenueCommissionService.recordCommissionForPayment(payment);
        billingFinanceService.issuePaymentReceipt(payment, bill, partial);
        auditComplianceService.log(
                "PAYMENT_SUCCESS",
                "PAYMENT",
                payment.getId(),
                "Payment " + payment.getProviderReference() + " succeeded for policy " + payment.getPolicyId());

        outboxPublisherService.publish(
                "PAYMENT",
                payment.getId(),
                "PAYMENT_SUCCESS",
                "{\"policyId\":\"" + payment.getPolicyId() + "\",\"amount\":\"" + payment.getAmount() + "\"}");
    }

    private CitizenProfile requireMyProfile() {
        return citizenProfileRepository
                .findByUserId(SecurityUtils.currentUser().getUserId())
                .orElseThrow(() -> new BusinessException("Citizen profile not found"));
    }

    private PaymentResponse toResponse(Payment payment) {
        return toResponse(payment, null);
    }

    private PaymentResponse toResponse(Payment payment, PaymentInitiationResult initiation) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .policyId(payment.getPolicyId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .provider(payment.getProvider())
                .providerReference(payment.getProviderReference())
                .externalTransactionId(initiation != null ? initiation.getExternalTransactionId() : null)
                .payerPhone(initiation != null ? initiation.getPayerPhone() : null)
                .paymentInstructions(initiation != null ? initiation.getInstructions() : null)
                .initiatedAt(payment.getInitiatedAt())
                .completedAt(payment.getCompletedAt())
                .build();
    }
}
