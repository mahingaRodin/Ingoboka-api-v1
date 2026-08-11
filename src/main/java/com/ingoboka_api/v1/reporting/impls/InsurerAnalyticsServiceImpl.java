package com.ingoboka_api.v1.reporting.impls;

import com.ingoboka_api.v1.claim.models.Claim;
import com.ingoboka_api.v1.claim.repositories.ClaimRepository;
import com.ingoboka_api.v1.common.enums.ApplicationStatus;
import com.ingoboka_api.v1.common.enums.ClaimStatus;
import com.ingoboka_api.v1.common.enums.PolicyStatus;
import com.ingoboka_api.v1.common.enums.RevenueEntryType;
import com.ingoboka_api.v1.common.enums.RevenueLedgerStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.responses.InsurerDashboardResponse;
import com.ingoboka_api.v1.common.responses.RevenueTrendResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.enrollment.repositories.PolicyApplicationRepository;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import com.ingoboka_api.v1.reporting.services.InsurerAnalyticsService;
import com.ingoboka_api.v1.revenue.models.RevenueLedgerEntry;
import com.ingoboka_api.v1.revenue.repositories.RevenueLedgerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsurerAnalyticsServiceImpl implements InsurerAnalyticsService {

    private static final EnumSet<ClaimStatus> OPEN_CLAIM_STATUSES = EnumSet.of(
            ClaimStatus.SUBMITTED,
            ClaimStatus.UNDER_REVIEW,
            ClaimStatus.INFORMATION_REQUIRED,
            ClaimStatus.PAYMENT_PROCESSING);

    private static final Map<String, String> DISTRICT_TO_PROVINCE = Map.ofEntries(
            Map.entry("Gasabo", "City of Kigali"),
            Map.entry("Kicukiro", "City of Kigali"),
            Map.entry("Nyarugenge", "City of Kigali"),
            Map.entry("Bugesera", "Eastern Province"),
            Map.entry("Gatsibo", "Eastern Province"),
            Map.entry("Kayonza", "Eastern Province"),
            Map.entry("Kirehe", "Eastern Province"),
            Map.entry("Ngoma", "Eastern Province"),
            Map.entry("Nyagatare", "Eastern Province"),
            Map.entry("Rwamagana", "Eastern Province"),
            Map.entry("Gicumbi", "Northern Province"),
            Map.entry("Burera", "Northern Province"),
            Map.entry("Gakenke", "Northern Province"),
            Map.entry("Musanze", "Northern Province"),
            Map.entry("Rulindo", "Northern Province"),
            Map.entry("Karongi", "Western Province"),
            Map.entry("Ngororero", "Western Province"),
            Map.entry("Nyabihu", "Western Province"),
            Map.entry("Nyamasheke", "Western Province"),
            Map.entry("Rubavu", "Western Province"),
            Map.entry("Rusizi", "Western Province"),
            Map.entry("Rutsiro", "Western Province"),
            Map.entry("Huye", "Southern Province"),
            Map.entry("Gisagara", "Southern Province"),
            Map.entry("Kamonyi", "Southern Province"),
            Map.entry("Muhanga", "Southern Province"),
            Map.entry("Nyamagabe", "Southern Province"),
            Map.entry("Nyanza", "Southern Province"),
            Map.entry("Nyaruguru", "Southern Province"),
            Map.entry("Ruhango", "Southern Province"));

    private static final Map<String, String> DISTRICT_CODES = Map.ofEntries(
            Map.entry("Gasabo", "KV-GAS"),
            Map.entry("Kicukiro", "KV-KIC"),
            Map.entry("Nyarugenge", "KV-NYA"),
            Map.entry("Bugesera", "EP-BUG"),
            Map.entry("Gatsibo", "EP-GAT"),
            Map.entry("Kayonza", "EP-KAY"),
            Map.entry("Kirehe", "EP-KIR"),
            Map.entry("Ngoma", "EP-NGO"),
            Map.entry("Nyagatare", "EP-NYA"),
            Map.entry("Rwamagana", "EP-RWA"),
            Map.entry("Gicumbi", "NP-GIC"),
            Map.entry("Burera", "NP-BUR"),
            Map.entry("Gakenke", "NP-GAK"),
            Map.entry("Musanze", "NP-MUS"),
            Map.entry("Rulindo", "NP-RUL"),
            Map.entry("Karongi", "WP-KAR"),
            Map.entry("Ngororero", "WP-NGO"),
            Map.entry("Nyabihu", "WP-NYB"),
            Map.entry("Nyamasheke", "WP-NYS"),
            Map.entry("Rubavu", "WP-RUB"),
            Map.entry("Rusizi", "WP-RUS"),
            Map.entry("Rutsiro", "WP-RUT"),
            Map.entry("Huye", "SP-HUY"),
            Map.entry("Gisagara", "SP-GIS"),
            Map.entry("Kamonyi", "SP-KAM"),
            Map.entry("Muhanga", "SP-MUH"),
            Map.entry("Nyamagabe", "SP-NYM"),
            Map.entry("Nyanza", "SP-NYZ"),
            Map.entry("Nyaruguru", "SP-NYR"),
            Map.entry("Ruhango", "SP-RUH"));

    private final PolicyRepository policyRepository;
    private final PolicyApplicationRepository policyApplicationRepository;
    private final ClaimRepository claimRepository;
    private final RevenueLedgerRepository revenueLedgerRepository;

    @Override
    @Transactional(readOnly = true)
    public InsurerDashboardResponse getDashboard() {
        UUID orgId = requireTenantOrganizationId();

        long activePolicies = policyRepository.countByOrganizationIdAndStatus(orgId, PolicyStatus.ACTIVE);
        long citizensEnrolled =
                policyRepository.countDistinctCitizensByOrganizationIdAndStatus(orgId, PolicyStatus.ACTIVE);
        long openClaims = claimRepository.countByOrganizationIdAndStatusIn(orgId, OPEN_CLAIM_STATUSES);
        long pendingApplications = policyApplicationRepository.countByOrganizationIdAndStatus(
                        orgId, ApplicationStatus.SUBMITTED)
                + policyApplicationRepository.countByOrganizationIdAndStatus(orgId, ApplicationStatus.UNDER_REVIEW);

        List<Claim> claims = claimRepository.findByOrganizationIdAndStatusNotOrderByCreatedAtDesc(orgId, ClaimStatus.DRAFT);
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        long resolvedToday = claims.stream()
                .filter(claim -> claim.getStatus() == ClaimStatus.APPROVED || claim.getStatus() == ClaimStatus.REJECTED)
                .filter(claim -> claim.getUpdatedAt() != null && claim.getUpdatedAt().isAfter(startOfDay))
                .count();
        double avgResolutionDays = claims.stream()
                .filter(claim -> claim.getStatus() == ClaimStatus.APPROVED || claim.getStatus() == ClaimStatus.REJECTED)
                .filter(claim -> claim.getCreatedAt() != null && claim.getUpdatedAt() != null)
                .mapToLong(claim -> ChronoUnit.DAYS.between(claim.getCreatedAt(), claim.getUpdatedAt()))
                .average()
                .orElse(0.0);

        List<InsurerDashboardResponse.StatusCount> claimsByStatus = Arrays.stream(ClaimStatus.values())
                .map(status -> InsurerDashboardResponse.StatusCount.builder()
                        .status(status.name())
                        .count(claims.stream().filter(claim -> claim.getStatus() == status).count())
                        .build())
                .filter(item -> item.getCount() > 0)
                .toList();

        List<InsurerDashboardResponse.NamedCount> enrollmentByProduct =
                policyRepository.countEnrolledByProduct(orgId, PolicyStatus.ACTIVE).stream()
                        .map(row -> InsurerDashboardResponse.NamedCount.builder()
                                .name(String.valueOf(row[0]))
                                .count(((Number) row[1]).longValue())
                                .build())
                        .toList();

        List<InsurerDashboardResponse.GeographyCount> enrollmentByDistrict =
                policyRepository.countEnrolledByDistrict(orgId, PolicyStatus.ACTIVE).stream()
                        .map(row -> {
                            String district = String.valueOf(row[0]);
                            long enrolled = ((Number) row[1]).longValue();
                            return InsurerDashboardResponse.GeographyCount.builder()
                                    .district(district)
                                    .province(DISTRICT_TO_PROVINCE.getOrDefault(district, "Rwanda"))
                                    .districtCode(DISTRICT_CODES.getOrDefault(district, district))
                                    .enrolled(enrolled)
                                    .build();
                        })
                        .toList();

        return InsurerDashboardResponse.builder()
                .activePolicies(activePolicies)
                .citizensEnrolled(citizensEnrolled)
                .openClaims(openClaims)
                .pendingApplications(pendingApplications)
                .resolvedToday(resolvedToday)
                .avgResolutionDays(avgResolutionDays)
                .claimsByStatus(claimsByStatus)
                .enrollmentByProduct(enrollmentByProduct)
                .enrollmentByDistrict(enrollmentByDistrict)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueTrendResponse getRevenueTrends(String granularity) {
        UUID orgId = requireTenantOrganizationId();
        String mode = granularity == null ? "monthly" : granularity.toLowerCase();
        List<RevenueLedgerEntry> entries = revenueLedgerRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);

        Map<String, PeriodAccumulator> buckets = new LinkedHashMap<>();
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yyyy");
        DateTimeFormatter weekFmt = DateTimeFormatter.ofPattern("'W'w yyyy");
        DateTimeFormatter yearFmt = DateTimeFormatter.ofPattern("yyyy");

        for (RevenueLedgerEntry entry : entries) {
            LocalDate date = entry.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate();
            String label;
            String periodStart;
            String periodEnd;
            switch (mode) {
                case "weekly" -> {
                    int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                    int year = date.get(IsoFields.WEEK_BASED_YEAR);
                    label = "W" + week + " " + year;
                    LocalDate weekStart = date.with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
                            .with(java.time.DayOfWeek.MONDAY);
                    periodStart = weekStart.toString();
                    periodEnd = weekStart.plusDays(6).toString();
                }
                case "yearly" -> {
                    label = yearFmt.format(date);
                    periodStart = date.withDayOfYear(1).toString();
                    periodEnd = date.withDayOfYear(date.lengthOfYear()).toString();
                }
                default -> {
                    label = monthFmt.format(date);
                    periodStart = date.withDayOfMonth(1).toString();
                    periodEnd = date.withDayOfMonth(date.lengthOfMonth()).toString();
                }
            }
            PeriodAccumulator acc = buckets.computeIfAbsent(label, k -> new PeriodAccumulator(label, periodStart, periodEnd));
            BigDecimal amount = entry.getAmount() != null ? entry.getAmount() : BigDecimal.ZERO;
            acc.revenue = acc.revenue.add(amount);
            if (entry.getStatus() == RevenueLedgerStatus.SETTLED) {
                acc.settled = acc.settled.add(amount);
            } else {
                acc.pending = acc.pending.add(amount);
            }
            if (entry.getEntryType() == RevenueEntryType.COMMISSION
                    || entry.getEntryType() == RevenueEntryType.ADJUSTMENT) {
                acc.spending = acc.spending.add(amount);
            }
        }

        List<RevenueTrendResponse.PeriodPoint> periods = new ArrayList<>(buckets.values()).stream()
                .map(acc -> RevenueTrendResponse.PeriodPoint.builder()
                        .label(acc.label)
                        .periodStart(acc.periodStart)
                        .periodEnd(acc.periodEnd)
                        .revenue(acc.revenue)
                        .settled(acc.settled)
                        .pending(acc.pending)
                        .spending(acc.spending)
                        .invoiceCount(0)
                        .build())
                .toList();

        BigDecimal totalRevenue = periods.stream()
                .map(RevenueTrendResponse.PeriodPoint::getRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSettled = periods.stream()
                .map(RevenueTrendResponse.PeriodPoint::getSettled)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalPending = periods.stream()
                .map(RevenueTrendResponse.PeriodPoint::getPending)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpending = periods.stream()
                .map(RevenueTrendResponse.PeriodPoint::getSpending)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RevenueTrendResponse.builder()
                .granularity(mode)
                .totalRevenue(totalRevenue)
                .totalSettled(totalSettled)
                .totalPending(totalPending)
                .totalSpending(totalSpending)
                .periods(periods)
                .build();
    }

    private UUID requireTenantOrganizationId() {
        var user = SecurityUtils.currentUser();
        if (user.getOrganizationId() == null) {
            throw new BusinessException("No organization associated with this account");
        }
        return user.getOrganizationId();
    }

    private static final class PeriodAccumulator {
        private final String label;
        private final String periodStart;
        private final String periodEnd;
        private BigDecimal revenue = BigDecimal.ZERO;
        private BigDecimal settled = BigDecimal.ZERO;
        private BigDecimal pending = BigDecimal.ZERO;
        private BigDecimal spending = BigDecimal.ZERO;

        private PeriodAccumulator(String label, String periodStart, String periodEnd) {
            this.label = label;
            this.periodStart = periodStart;
            this.periodEnd = periodEnd;
        }
    }
}
