package com.ingoboka_api.v1.billing.impls;

import com.ingoboka_api.v1.billing.models.Payment;
import com.ingoboka_api.v1.billing.models.PaymentEvent;
import com.ingoboka_api.v1.billing.models.PremiumSchedule;
import com.ingoboka_api.v1.billing.repositories.PaymentEventRepository;
import com.ingoboka_api.v1.billing.repositories.PaymentRepository;
import com.ingoboka_api.v1.billing.repositories.PremiumScheduleRepository;
import com.ingoboka_api.v1.billing.services.BillingService;
import com.ingoboka_api.v1.common.enums.PaymentStatus;
import com.ingoboka_api.v1.common.enums.PremiumScheduleStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.InitiatePaymentRequest;
import com.ingoboka_api.v1.common.requests.PaymentWebhookRequest;
import com.ingoboka_api.v1.common.responses.PaymentResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.customer.models.CitizenProfile;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.policy.models.Policy;
import com.ingoboka_api.v1.policy.services.PolicyIssuanceService;
import com.ingoboka_api.v1.policy.services.PolicyService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final PaymentRepository paymentRepository;
    private final PaymentEventRepository paymentEventRepository;
    private final PremiumScheduleRepository premiumScheduleRepository;
    private final PolicyService policyService;
    private final PolicyIssuanceService policyIssuanceService;
    private final CitizenProfileRepository citizenProfileRepository;

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

        Instant now = Instant.now();
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setOrganizationId(policy.getOrganizationId());
        payment.setPolicyId(policy.getId());
        payment.setCitizenProfileId(profile.getId());
        payment.setAmount(policy.getPremiumAmount());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setProvider("SANDBOX");
        payment.setProviderReference("PAY-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
        payment.setIdempotencyKey(idempotencyKey);
        payment.setInitiatedAt(now);
        paymentRepository.save(payment);

        return toResponse(payment);
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
    public List<PaymentResponse> listMyPayments() {
        CitizenProfile profile = requireMyProfile();
        return paymentRepository.findByCitizenProfileIdOrderByInitiatedAtDesc(profile.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> listPolicyPayments(UUID policyId) {
        policyService.getPolicy(policyId);
        return paymentRepository.findByPolicyIdOrderByInitiatedAtDesc(policyId).stream()
                .map(this::toResponse)
                .toList();
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

    private CitizenProfile requireMyProfile() {
        return citizenProfileRepository
                .findByUserId(SecurityUtils.currentUser().getUserId())
                .orElseThrow(() -> new BusinessException("Citizen profile not found"));
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .policyId(payment.getPolicyId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .provider(payment.getProvider())
                .providerReference(payment.getProviderReference())
                .initiatedAt(payment.getInitiatedAt())
                .completedAt(payment.getCompletedAt())
                .build();
    }
}
