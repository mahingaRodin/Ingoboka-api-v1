package com.ingoboka_api.v1.reporting.impls;

import com.ingoboka_api.v1.billing.repositories.PaymentRepository;
import com.ingoboka_api.v1.claim.repositories.ClaimRepository;
import com.ingoboka_api.v1.common.enums.ApplicationStatus;
import com.ingoboka_api.v1.common.enums.ClaimStatus;
import com.ingoboka_api.v1.common.enums.PaymentStatus;
import com.ingoboka_api.v1.common.enums.PolicyStatus;
import com.ingoboka_api.v1.common.enums.RevenueLedgerStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.TenantOverviewResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.common.util.PaginationUtils;
import com.ingoboka_api.v1.enrollment.repositories.PolicyApplicationRepository;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import com.ingoboka_api.v1.reporting.services.ReportingService;
import com.ingoboka_api.v1.revenue.repositories.RevenueLedgerRepository;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportingServiceImpl implements ReportingService {

    private static final EnumSet<ClaimStatus> OPEN_CLAIM_STATUSES = EnumSet.of(
            ClaimStatus.SUBMITTED,
            ClaimStatus.UNDER_REVIEW,
            ClaimStatus.INFORMATION_REQUIRED,
            ClaimStatus.PAYMENT_PROCESSING);

    private final PolicyRepository policyRepository;
    private final PolicyApplicationRepository policyApplicationRepository;
    private final ClaimRepository claimRepository;
    private final PaymentRepository paymentRepository;
    private final RevenueLedgerRepository revenueLedgerRepository;

    @Override
    @Transactional(readOnly = true)
    public TenantOverviewResponse getTenantOverview() {
        UUID orgId = requireTenantOrganizationId();

        long activePolicies = policyRepository.countByOrganizationIdAndStatus(orgId, PolicyStatus.ACTIVE);
        long pendingApplications = policyApplicationRepository.countByOrganizationIdAndStatus(
                orgId, ApplicationStatus.SUBMITTED)
                + policyApplicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.UNDER_REVIEW);
        long openClaims = claimRepository.countByOrganizationIdAndStatusIn(orgId, OPEN_CLAIM_STATUSES);
        long successfulPayments =
                paymentRepository.countByOrganizationIdAndStatus(orgId, PaymentStatus.SUCCESS);
        BigDecimal pendingRevenue =
                revenueLedgerRepository.sumAmountByOrganizationIdAndStatus(orgId, RevenueLedgerStatus.PENDING);
        BigDecimal settledRevenue =
                revenueLedgerRepository.sumAmountByOrganizationIdAndStatus(orgId, RevenueLedgerStatus.SETTLED);

        return TenantOverviewResponse.builder()
                .activePolicies(activePolicies)
                .pendingApplications(pendingApplications)
                .openClaims(openClaims)
                .successfulPayments(successfulPayments)
                .pendingRevenue(pendingRevenue)
                .settledRevenue(settledRevenue)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<?> getPolicyReport(int page, int size) {
        UUID orgId = requireTenantOrganizationId();
        var result = policyRepository.findByOrganizationIdOrderByCreatedAtDesc(
                orgId, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(p -> java.util.Map.of(
                "id", p.getId(),
                "policyNumber", p.getPolicyNumber(),
                "status", p.getStatus().name(),
                "premiumAmount", p.getPremiumAmount(),
                "startDate", String.valueOf(p.getStartDate()),
                "endDate", String.valueOf(p.getEndDate()))));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<?> getClaimReport(int page, int size) {
        UUID orgId = requireTenantOrganizationId();
        var result = claimRepository.findByOrganizationIdOrderByCreatedAtDesc(
                orgId, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(c -> java.util.Map.of(
                "id", c.getId(),
                "claimNumber", c.getClaimNumber(),
                "status", c.getStatus().name(),
                "claimedAmount", c.getClaimedAmount(),
                "createdAt", c.getCreatedAt().toString())));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<?> getPaymentReport(int page, int size) {
        UUID orgId = requireTenantOrganizationId();
        var result = paymentRepository.findByOrganizationIdOrderByInitiatedAtDesc(
                orgId, PaginationUtils.toPageable(page, size, "initiatedAt"));
        return PageResponse.from(result.map(p -> java.util.Map.of(
                "id", p.getId(),
                "policyId", p.getPolicyId(),
                "amount", p.getAmount(),
                "status", p.getStatus().name(),
                "providerReference", p.getProviderReference())));
    }

    @Override
    @Transactional(readOnly = true)
    public String exportPoliciesCsv() {
        UUID orgId = requireTenantOrganizationId();
        String header = "policyNumber,status,premiumAmount,startDate,endDate\n";
        String rows = policyRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(p -> p.getPolicyNumber() + ","
                        + p.getStatus() + ","
                        + p.getPremiumAmount() + ","
                        + p.getStartDate() + ","
                        + p.getEndDate())
                .collect(Collectors.joining("\n"));
        return header + rows;
    }

    @Override
    @Transactional(readOnly = true)
    public String exportClaimsCsv() {
        UUID orgId = requireTenantOrganizationId();
        String header = "claimNumber,status,claimedAmount,createdAt\n";
        String rows = claimRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(c -> c.getClaimNumber() + ","
                        + c.getStatus() + ","
                        + c.getClaimedAmount() + ","
                        + c.getCreatedAt())
                .collect(Collectors.joining("\n"));
        return header + rows;
    }

    @Override
    @Transactional(readOnly = true)
    public String exportPaymentsCsv() {
        UUID orgId = requireTenantOrganizationId();
        String header = "providerReference,policyId,amount,status,completedAt\n";
        String rows = paymentRepository.findByOrganizationIdOrderByInitiatedAtDesc(orgId).stream()
                .map(p -> p.getProviderReference() + ","
                        + p.getPolicyId() + ","
                        + p.getAmount() + ","
                        + p.getStatus() + ","
                        + p.getCompletedAt())
                .collect(Collectors.joining("\n"));
        return header + rows;
    }

    private UUID requireTenantOrganizationId() {
        var user = SecurityUtils.currentUser();
        if (user.getOrganizationId() == null) {
            throw new BusinessException("No organization associated with this account");
        }
        return user.getOrganizationId();
    }
}
