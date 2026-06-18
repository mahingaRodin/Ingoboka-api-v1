package com.ingoboka_api.v1.revenue.impls;

import com.ingoboka_api.v1.billing.models.Payment;
import com.ingoboka_api.v1.common.enums.ContractPriceRuleType;
import com.ingoboka_api.v1.common.enums.InvoiceStatus;
import com.ingoboka_api.v1.common.enums.RateType;
import com.ingoboka_api.v1.common.enums.RevenueEntryType;
import com.ingoboka_api.v1.common.enums.RevenueLedgerStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.CreateContractPriceRuleRequest;
import com.ingoboka_api.v1.common.responses.ContractPriceRuleResponse;
import com.ingoboka_api.v1.common.responses.InvoiceResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.RevenueLedgerResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.common.util.PaginationUtils;
import com.ingoboka_api.v1.revenue.models.ContractPriceRule;
import com.ingoboka_api.v1.revenue.models.Invoice;
import com.ingoboka_api.v1.revenue.models.RevenueLedgerEntry;
import com.ingoboka_api.v1.revenue.repositories.ContractPriceRuleRepository;
import com.ingoboka_api.v1.revenue.repositories.InvoiceRepository;
import com.ingoboka_api.v1.revenue.repositories.RevenueLedgerRepository;
import com.ingoboka_api.v1.revenue.services.RevenueCommissionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RevenueCommissionServiceImpl implements RevenueCommissionService {

    private final ContractPriceRuleRepository contractPriceRuleRepository;
    private final RevenueLedgerRepository revenueLedgerRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional
    public ContractPriceRuleResponse createPriceRule(CreateContractPriceRuleRequest request) {
        UUID orgId = requireTenantOrganizationId();
        Instant now = Instant.now();
        ContractPriceRule rule = new ContractPriceRule();
        rule.setId(UUID.randomUUID());
        rule.setOrganizationId(orgId);
        rule.setContractId(request.getContractId());
        rule.setRuleType(request.getRuleType());
        rule.setRateType(request.getRateType());
        rule.setRateValue(request.getRateValue());
        rule.setEffectiveFrom(request.getEffectiveFrom());
        rule.setEffectiveTo(request.getEffectiveTo());
        rule.setActive(true);
        rule.setCreatedAt(now);
        return toRuleResponse(contractPriceRuleRepository.save(rule));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContractPriceRuleResponse> listPriceRules(int page, int size) {
        UUID orgId = requireTenantOrganizationId();
        Page<ContractPriceRule> result = contractPriceRuleRepository.findByOrganizationIdOrderByCreatedAtDesc(
                orgId, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toRuleResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RevenueLedgerResponse> listLedger(int page, int size) {
        UUID orgId = requireTenantOrganizationId();
        Page<RevenueLedgerEntry> result = revenueLedgerRepository.findByOrganizationIdOrderByCreatedAtDesc(
                orgId, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toLedgerResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceResponse> listInvoices(int page, int size) {
        UUID orgId = requireTenantOrganizationId();
        Page<Invoice> result =
                invoiceRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toInvoiceResponse));
    }

    @Override
    @Transactional
    public InvoiceResponse generateInvoice(LocalDate periodStart, LocalDate periodEnd) {
        UUID orgId = requireTenantOrganizationId();
        BigDecimal total = revenueLedgerRepository.sumAmountByOrganizationIdAndStatus(orgId, RevenueLedgerStatus.PENDING);
        Instant now = Instant.now();
        Invoice invoice = new Invoice();
        invoice.setId(UUID.randomUUID());
        invoice.setOrganizationId(orgId);
        invoice.setInvoiceNumber("INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        invoice.setPeriodStart(periodStart);
        invoice.setPeriodEnd(periodEnd);
        invoice.setAmount(total);
        invoice.setStatus(InvoiceStatus.ISSUED);
        invoice.setIssuedAt(now);
        invoice.setCreatedAt(now);
        invoiceRepository.save(invoice);
        return toInvoiceResponse(invoice);
    }

    @Override
    @Transactional
    public void recordCommissionForPayment(Payment payment) {
        List<ContractPriceRule> rules =
                contractPriceRuleRepository.findByOrganizationIdAndActiveTrue(payment.getOrganizationId());
        LocalDate today = LocalDate.now();

        for (ContractPriceRule rule : rules) {
            if (rule.getEffectiveFrom().isAfter(today)) {
                continue;
            }
            if (rule.getEffectiveTo() != null && rule.getEffectiveTo().isBefore(today)) {
                continue;
            }
            if (rule.getRuleType() != ContractPriceRuleType.COMMISSION
                    && rule.getRuleType() != ContractPriceRuleType.PLATFORM_FEE
                    && rule.getRuleType() != ContractPriceRuleType.PER_POLICY_FEE) {
                continue;
            }

            BigDecimal amount = rule.getRateType() == RateType.PERCENTAGE
                    ? payment.getAmount()
                            .multiply(rule.getRateValue())
                            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    : rule.getRateValue();

            RevenueLedgerEntry entry = new RevenueLedgerEntry();
            entry.setId(UUID.randomUUID());
            entry.setOrganizationId(payment.getOrganizationId());
            entry.setPolicyId(payment.getPolicyId());
            entry.setPaymentId(payment.getId());
            entry.setEntryType(mapEntryType(rule.getRuleType()));
            entry.setAmount(amount);
            entry.setStatus(RevenueLedgerStatus.PENDING);
            entry.setReference("REV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            entry.setNotes("Auto-calculated from price rule " + rule.getId());
            entry.setCreatedAt(Instant.now());
            revenueLedgerRepository.save(entry);
        }
    }

    private RevenueEntryType mapEntryType(ContractPriceRuleType ruleType) {
        return switch (ruleType) {
            case COMMISSION -> RevenueEntryType.COMMISSION;
            case PLATFORM_FEE -> RevenueEntryType.PLATFORM_FEE;
            case PER_POLICY_FEE -> RevenueEntryType.POLICY_FEE;
            case SUBSCRIPTION -> RevenueEntryType.SUBSCRIPTION;
        };
    }

    private UUID requireTenantOrganizationId() {
        var user = SecurityUtils.currentUser();
        if (user.getOrganizationId() == null) {
            throw new BusinessException("No organization associated with this account");
        }
        return user.getOrganizationId();
    }

    private ContractPriceRuleResponse toRuleResponse(ContractPriceRule rule) {
        return ContractPriceRuleResponse.builder()
                .id(rule.getId())
                .organizationId(rule.getOrganizationId())
                .contractId(rule.getContractId())
                .ruleType(rule.getRuleType())
                .rateType(rule.getRateType())
                .rateValue(rule.getRateValue())
                .currency(rule.getCurrency())
                .effectiveFrom(rule.getEffectiveFrom())
                .effectiveTo(rule.getEffectiveTo())
                .active(rule.isActive())
                .createdAt(rule.getCreatedAt())
                .build();
    }

    private RevenueLedgerResponse toLedgerResponse(RevenueLedgerEntry entry) {
        return RevenueLedgerResponse.builder()
                .id(entry.getId())
                .organizationId(entry.getOrganizationId())
                .policyId(entry.getPolicyId())
                .paymentId(entry.getPaymentId())
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .currency(entry.getCurrency())
                .status(entry.getStatus())
                .reference(entry.getReference())
                .notes(entry.getNotes())
                .createdAt(entry.getCreatedAt())
                .build();
    }

    private InvoiceResponse toInvoiceResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .organizationId(invoice.getOrganizationId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .periodStart(invoice.getPeriodStart())
                .periodEnd(invoice.getPeriodEnd())
                .amount(invoice.getAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .issuedAt(invoice.getIssuedAt())
                .createdAt(invoice.getCreatedAt())
                .build();
    }
}
