package com.ingoboka_api.v1.policy.impls;

import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import com.ingoboka_api.v1.billing.models.Bill;
import com.ingoboka_api.v1.billing.models.PremiumSchedule;
import com.ingoboka_api.v1.billing.repositories.BillRepository;
import com.ingoboka_api.v1.billing.repositories.PremiumScheduleRepository;
import com.ingoboka_api.v1.billing.services.BillingFinanceService;
import com.ingoboka_api.v1.common.config.PolicyLifecycleProperties;
import com.ingoboka_api.v1.common.enums.BillStatus;
import com.ingoboka_api.v1.common.enums.NotificationChannel;
import com.ingoboka_api.v1.common.enums.PolicyStatus;
import com.ingoboka_api.v1.common.enums.PremiumScheduleStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.responses.PolicyResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.messaging.services.NotificationTemplateService;
import com.ingoboka_api.v1.policy.models.Policy;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import com.ingoboka_api.v1.policy.services.PolicyLifecycleService;
import com.ingoboka_api.v1.policy.services.PolicyService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyLifecycleServiceImpl implements PolicyLifecycleService {

    private final PolicyRepository policyRepository;
    private final PremiumScheduleRepository premiumScheduleRepository;
    private final BillRepository billRepository;
    private final BillingFinanceService billingFinanceService;
    private final PolicyLifecycleProperties lifecycleProperties;
    private final NotificationTemplateService notificationTemplateService;
    private final CitizenProfileRepository citizenProfileRepository;
    private final UserRepository userRepository;
    private final AuditComplianceService auditComplianceService;
    private final PolicyService policyService;

    @Override
    @Transactional
    public PolicyResponse renewPolicy(UUID policyId) {
        Policy policy = policyRepository
                .findById(policyId)
                .orElseThrow(() -> new BusinessException("Policy not found"));
        if (policy.getStatus() != PolicyStatus.ACTIVE
                && policy.getStatus() != PolicyStatus.GRACE_PERIOD
                && policy.getStatus() != PolicyStatus.LAPSED) {
            throw new BusinessException("Policy cannot be renewed in status " + policy.getStatus());
        }

        LocalDate newStart = LocalDate.now();
        policy.setStatus(PolicyStatus.PENDING_PAYMENT);
        policy.setStartDate(newStart);
        policy.setEndDate(newStart.plusYears(1));
        policy.setGracePeriodEnd(null);
        policy.setUpdatedAt(Instant.now());
        policyRepository.save(policy);

        PremiumSchedule schedule = new PremiumSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setPolicyId(policy.getId());
        schedule.setDueDate(newStart);
        schedule.setAmount(policy.getPremiumAmount());
        schedule.setStatus(PremiumScheduleStatus.PENDING);
        schedule.setCreatedAt(Instant.now());
        premiumScheduleRepository.save(schedule);
        billingFinanceService.issueBillForSchedule(schedule, policy.getOrganizationId(), policy.getId());

        auditComplianceService.log("POLICY_RENEWAL_INITIATED", "POLICY", policy.getId(), "Renewal premium due");
        return policyService.getPolicy(policyId);
    }

    @Override
    @Transactional
    public void processDailyLifecycle() {
        LocalDate today = LocalDate.now();
        issueDueBills(today);
        markOverdueSchedules(today);
        moveToGracePeriod(today);
        moveToLapsed(today);
        expirePolicies(today);
    }

    private void issueDueBills(LocalDate today) {
        List<PremiumSchedule> dueSchedules =
                premiumScheduleRepository.findByStatusAndDueDateLessThanEqual(PremiumScheduleStatus.PENDING, today);
        for (PremiumSchedule schedule : dueSchedules) {
            policyRepository.findById(schedule.getPolicyId()).ifPresent(policy -> {
                if (policy.getStatus() == PolicyStatus.ACTIVE || policy.getStatus() == PolicyStatus.GRACE_PERIOD) {
                    billingFinanceService.issueBillForSchedule(schedule, policy.getOrganizationId(), policy.getId());
                }
            });
        }
    }

    private void markOverdueSchedules(LocalDate today) {
        premiumScheduleRepository
                .findByStatusAndDueDateBefore(PremiumScheduleStatus.PENDING, today)
                .forEach(schedule -> {
                    schedule.setStatus(PremiumScheduleStatus.OVERDUE);
                    premiumScheduleRepository.save(schedule);
                    billRepository
                            .findFirstByPremiumScheduleId(schedule.getId())
                            .ifPresent(bill -> {
                                bill.setStatus(BillStatus.OVERDUE);
                                billRepository.save(bill);
                            });
                });
    }

    private void moveToGracePeriod(LocalDate today) {
        List<Policy> activePolicies = policyRepository.findByStatus(PolicyStatus.ACTIVE);
        for (Policy policy : activePolicies) {
            boolean hasOverdue = premiumScheduleRepository.existsByPolicyIdAndStatus(
                    policy.getId(), PremiumScheduleStatus.OVERDUE);
            if (hasOverdue && policy.getGracePeriodEnd() == null) {
                policy.setStatus(PolicyStatus.GRACE_PERIOD);
                policy.setGracePeriodEnd(today.plusDays(lifecycleProperties.getGracePeriodDays()));
                policy.setUpdatedAt(Instant.now());
                policyRepository.save(policy);
                notifyPolicyholder(policy, "POLICY_GRACE", Map.of(
                        "policyNumber", policy.getPolicyNumber(),
                        "graceEndDate", policy.getGracePeriodEnd().toString()));
                auditComplianceService.log(
                        "POLICY_GRACE_PERIOD", "POLICY", policy.getId(), "Entered grace period");
            }
        }
    }

    private void moveToLapsed(LocalDate today) {
        List<Policy> gracePolicies = policyRepository.findByStatus(PolicyStatus.GRACE_PERIOD);
        for (Policy policy : gracePolicies) {
            if (policy.getGracePeriodEnd() != null && policy.getGracePeriodEnd().isBefore(today)) {
                policy.setStatus(PolicyStatus.LAPSED);
                policy.setUpdatedAt(Instant.now());
                policyRepository.save(policy);
                notifyPolicyholder(policy, "POLICY_LAPSED", Map.of("policyNumber", policy.getPolicyNumber()));
                auditComplianceService.log("POLICY_LAPSED", "POLICY", policy.getId(), "Policy lapsed after grace");
            }
        }
    }

    private void expirePolicies(LocalDate today) {
        policyRepository.findByStatusAndEndDateBefore(PolicyStatus.ACTIVE, today).forEach(policy -> {
            policy.setStatus(PolicyStatus.EXPIRED);
            policy.setUpdatedAt(Instant.now());
            policyRepository.save(policy);
        });
        policyRepository.findByStatusAndEndDateBefore(PolicyStatus.GRACE_PERIOD, today).forEach(policy -> {
            policy.setStatus(PolicyStatus.EXPIRED);
            policy.setUpdatedAt(Instant.now());
            policyRepository.save(policy);
        });
    }

    private void notifyPolicyholder(Policy policy, String template, Map<String, String> vars) {
        citizenProfileRepository.findById(policy.getCitizenProfileId()).ifPresent(profile -> userRepository
                .findById(profile.getUserId())
                .ifPresent(user -> notificationTemplateService.sendTemplated(
                        user.getId(),
                        policy.getOrganizationId(),
                        template,
                        NotificationChannel.EMAIL,
                        user.getEmail(),
                        vars)));
    }
}
