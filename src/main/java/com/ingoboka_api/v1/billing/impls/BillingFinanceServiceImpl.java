package com.ingoboka_api.v1.billing.impls;

import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import com.ingoboka_api.v1.billing.models.Bill;
import com.ingoboka_api.v1.billing.models.Payment;
import com.ingoboka_api.v1.billing.models.PremiumSchedule;
import com.ingoboka_api.v1.billing.models.Receipt;
import com.ingoboka_api.v1.billing.models.ReconciliationRecord;
import com.ingoboka_api.v1.billing.models.Refund;
import com.ingoboka_api.v1.billing.repositories.BillRepository;
import com.ingoboka_api.v1.billing.repositories.PaymentRepository;
import com.ingoboka_api.v1.billing.repositories.ReceiptRepository;
import com.ingoboka_api.v1.billing.repositories.ReconciliationRepository;
import com.ingoboka_api.v1.billing.repositories.RefundRepository;
import com.ingoboka_api.v1.billing.services.BillingFinanceService;
import com.ingoboka_api.v1.common.enums.BillStatus;
import com.ingoboka_api.v1.common.enums.NotificationChannel;
import com.ingoboka_api.v1.common.enums.PaymentStatus;
import com.ingoboka_api.v1.common.enums.ReceiptType;
import com.ingoboka_api.v1.common.enums.ReconciliationStatus;
import com.ingoboka_api.v1.common.enums.RefundStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.CreateRefundRequest;
import com.ingoboka_api.v1.common.responses.BillResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.ReceiptResponse;
import com.ingoboka_api.v1.common.responses.ReconciliationResponse;
import com.ingoboka_api.v1.common.responses.RefundResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.common.util.PaginationUtils;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.messaging.services.NotificationTemplateService;
import com.ingoboka_api.v1.policy.models.Policy;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BillingFinanceServiceImpl implements BillingFinanceService {

    private final BillRepository billRepository;
    private final ReceiptRepository receiptRepository;
    private final RefundRepository refundRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final PaymentRepository paymentRepository;
    private final PolicyRepository policyRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final UserRepository userRepository;
    private final NotificationTemplateService notificationTemplateService;
    private final AuditComplianceService auditComplianceService;

    @Override
    @Transactional
    public Bill issueBillForSchedule(PremiumSchedule schedule, UUID organizationId, UUID policyId) {
        if (billRepository.findFirstByPremiumScheduleId(schedule.getId()).isPresent()) {
            return billRepository.findFirstByPremiumScheduleId(schedule.getId()).orElseThrow();
        }
        Instant now = Instant.now();
        Bill bill = new Bill();
        bill.setId(UUID.randomUUID());
        bill.setOrganizationId(organizationId);
        bill.setPolicyId(policyId);
        bill.setPremiumScheduleId(schedule.getId());
        bill.setBillNumber("BILL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        bill.setAmount(schedule.getAmount());
        bill.setDueDate(schedule.getDueDate());
        bill.setStatus(BillStatus.ISSUED);
        bill.setIssuedAt(now);
        bill.setCreatedAt(now);
        billRepository.save(bill);

        notifyBillIssued(policyId, bill);
        return bill;
    }

    @Override
    @Transactional
    public Receipt issuePaymentReceipt(Payment payment, Bill bill, boolean partial) {
        Instant now = Instant.now();
        Receipt receipt = new Receipt();
        receipt.setId(UUID.randomUUID());
        receipt.setOrganizationId(payment.getOrganizationId());
        receipt.setPolicyId(payment.getPolicyId());
        receipt.setPaymentId(payment.getId());
        receipt.setBillId(bill != null ? bill.getId() : null);
        receipt.setReceiptNumber("RCT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        receipt.setReceiptType(partial ? ReceiptType.PARTIAL_PAYMENT : ReceiptType.FULL_PAYMENT);
        receipt.setAmount(payment.getAmount());
        receipt.setIssuedAt(now);
        receipt.setCreatedAt(now);
        receiptRepository.save(receipt);

        if (bill != null) {
            BigDecimal paid = bill.getAmountPaid().add(payment.getAmount());
            bill.setAmountPaid(paid);
            if (paid.compareTo(bill.getAmount()) >= 0) {
                bill.setStatus(BillStatus.PAID);
            } else {
                bill.setStatus(BillStatus.PARTIALLY_PAID);
            }
            billRepository.save(bill);
        }

        notifyPaymentReceipt(payment, receipt, partial, bill);
        return receipt;
    }

    @Override
    @Transactional
    public Receipt issueRefundReceipt(UUID refundId) {
        Refund refund = refundRepository.findById(refundId).orElseThrow(() -> new BusinessException("Refund not found"));
        Payment payment = paymentRepository
                .findById(refund.getPaymentId())
                .orElseThrow(() -> new BusinessException("Payment not found"));
        Instant now = Instant.now();
        Receipt receipt = new Receipt();
        receipt.setId(UUID.randomUUID());
        receipt.setOrganizationId(payment.getOrganizationId());
        receipt.setPolicyId(payment.getPolicyId());
        receipt.setRefundId(refund.getId());
        receipt.setPaymentId(payment.getId());
        receipt.setReceiptNumber("RFD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        receipt.setReceiptType(ReceiptType.REFUND);
        receipt.setAmount(refund.getAmount());
        receipt.setIssuedAt(now);
        receipt.setCreatedAt(now);
        receiptRepository.save(receipt);
        notifyRefund(payment, refund, receipt);
        return receipt;
    }

    @Override
    @Transactional
    public RefundResponse createRefund(CreateRefundRequest request) {
        Payment payment = paymentRepository
                .findById(request.getPaymentId())
                .orElseThrow(() -> new BusinessException("Payment not found"));
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new BusinessException("Only successful payments can be refunded");
        }
        if (request.getAmount().compareTo(payment.getAmount()) > 0) {
            throw new BusinessException("Refund amount exceeds payment amount");
        }

        Instant now = Instant.now();
        Refund refund = new Refund();
        refund.setId(UUID.randomUUID());
        refund.setPaymentId(payment.getId());
        refund.setAmount(request.getAmount());
        refund.setReason(request.getReason());
        refund.setStatus(RefundStatus.COMPLETED);
        refund.setCreatedAt(now);
        refundRepository.save(refund);

        payment.setStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);
        issueRefundReceipt(refund.getId());

        auditComplianceService.log("REFUND_CREATED", "REFUND", refund.getId(), "Refund for payment " + payment.getId());
        return toRefundResponse(refund);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> listRefunds(UUID paymentId, int page, int size) {
        Page<Refund> result =
                refundRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toRefundResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BillResponse> listPolicyBills(UUID policyId, int page, int size) {
        Page<Bill> result =
                billRepository.findByPolicyIdOrderByIssuedAtDesc(policyId, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toBillResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReceiptResponse> listPolicyReceipts(UUID policyId, int page, int size) {
        Page<Receipt> result =
                receiptRepository.findByPolicyIdOrderByIssuedAtDesc(policyId, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toReceiptResponse));
    }

    @Override
    @Transactional
    public ReconciliationResponse runReconciliation(LocalDate date) {
        UUID orgId = requireTenantOrganizationId();
        BigDecimal payments = paymentRepository.sumSuccessfulByOrganizationAndDate(orgId, date);
        BigDecimal refunds = refundRepository.sumCompletedByOrganizationAndDate(orgId, date);

        ReconciliationRecord record = new ReconciliationRecord();
        record.setId(UUID.randomUUID());
        record.setOrganizationId(orgId);
        record.setReconciliationDate(date);
        record.setTotalPayments(payments);
        record.setTotalRefunds(refunds);
        record.setNetAmount(payments.subtract(refunds));
        record.setStatus(ReconciliationStatus.CONFIRMED);
        record.setCreatedAt(Instant.now());
        reconciliationRepository.save(record);
        return toReconciliationResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReconciliationResponse> listReconciliations(int page, int size) {
        UUID orgId = requireTenantOrganizationId();
        Page<ReconciliationRecord> result = reconciliationRepository.findByOrganizationIdOrderByReconciliationDateDesc(
                orgId, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toReconciliationResponse));
    }

    private void notifyBillIssued(UUID policyId, Bill bill) {
        policyRepository.findById(policyId).ifPresent(policy -> sendToPolicyholder(
                policy,
                "BILL_ISSUED",
                Map.of(
                        "policyNumber", policy.getPolicyNumber(),
                        "billNumber", bill.getBillNumber(),
                        "amount", bill.getAmount().toPlainString(),
                        "currency", bill.getCurrency(),
                        "dueDate", bill.getDueDate().toString())));
    }

    private void notifyPaymentReceipt(Payment payment, Receipt receipt, boolean partial, Bill bill) {
        policyRepository.findById(payment.getPolicyId()).ifPresent(policy -> {
            String template = partial ? "PAYMENT_PARTIAL" : "PAYMENT_SUCCESS";
            Map<String, String> vars = new java.util.HashMap<>(Map.of(
                    "policyNumber", policy.getPolicyNumber(),
                    "amount", payment.getAmount().toPlainString(),
                    "currency", payment.getCurrency(),
                    "receiptNumber", receipt.getReceiptNumber()));
            if (partial && bill != null) {
                vars.put("remaining", bill.getAmount().subtract(bill.getAmountPaid()).toPlainString());
            }
            sendToPolicyholder(policy, template, vars);
        });
    }

    private void notifyRefund(Payment payment, Refund refund, Receipt receipt) {
        policyRepository.findById(payment.getPolicyId()).ifPresent(policy -> sendToPolicyholder(
                policy,
                "REFUND_PROCESSED",
                Map.of(
                        "policyNumber", policy.getPolicyNumber(),
                        "amount", refund.getAmount().toPlainString(),
                        "currency", payment.getCurrency())));
    }

    private void sendToPolicyholder(Policy policy, String templateCode, Map<String, String> variables) {
        citizenProfileRepository.findById(policy.getCitizenProfileId()).ifPresent(profile -> userRepository
                .findById(profile.getUserId())
                .ifPresent(user -> notificationTemplateService.sendTemplated(
                        user.getId(),
                        policy.getOrganizationId(),
                        templateCode,
                        NotificationChannel.EMAIL,
                        user.getEmail(),
                        variables)));
    }

    private UUID requireTenantOrganizationId() {
        var user = SecurityUtils.currentUser();
        if (user.getOrganizationId() == null) {
            throw new BusinessException("No organization associated with this account");
        }
        return user.getOrganizationId();
    }

    private BillResponse toBillResponse(Bill bill) {
        return BillResponse.builder()
                .id(bill.getId())
                .policyId(bill.getPolicyId())
                .billNumber(bill.getBillNumber())
                .amount(bill.getAmount())
                .amountPaid(bill.getAmountPaid())
                .currency(bill.getCurrency())
                .status(bill.getStatus())
                .dueDate(bill.getDueDate())
                .issuedAt(bill.getIssuedAt())
                .build();
    }

    private ReceiptResponse toReceiptResponse(Receipt receipt) {
        return ReceiptResponse.builder()
                .id(receipt.getId())
                .policyId(receipt.getPolicyId())
                .paymentId(receipt.getPaymentId())
                .refundId(receipt.getRefundId())
                .receiptNumber(receipt.getReceiptNumber())
                .receiptType(receipt.getReceiptType())
                .amount(receipt.getAmount())
                .currency(receipt.getCurrency())
                .issuedAt(receipt.getIssuedAt())
                .build();
    }

    private RefundResponse toRefundResponse(Refund refund) {
        return RefundResponse.builder()
                .id(refund.getId())
                .paymentId(refund.getPaymentId())
                .amount(refund.getAmount())
                .reason(refund.getReason())
                .status(refund.getStatus())
                .createdAt(refund.getCreatedAt())
                .build();
    }

    private ReconciliationResponse toReconciliationResponse(ReconciliationRecord record) {
        return ReconciliationResponse.builder()
                .id(record.getId())
                .reconciliationDate(record.getReconciliationDate())
                .totalPayments(record.getTotalPayments())
                .totalRefunds(record.getTotalRefunds())
                .netAmount(record.getNetAmount())
                .status(record.getStatus())
                .notes(record.getNotes())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
