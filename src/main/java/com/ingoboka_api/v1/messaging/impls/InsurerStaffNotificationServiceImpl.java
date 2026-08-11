package com.ingoboka_api.v1.messaging.impls;

import com.ingoboka_api.v1.claim.models.Claim;
import com.ingoboka_api.v1.common.config.PlatformProperties;
import com.ingoboka_api.v1.common.enums.ClaimStatus;
import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.identity.models.Role;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.identity.services.NotificationService;
import com.ingoboka_api.v1.messaging.services.InsurerStaffNotificationService;
import com.ingoboka_api.v1.messaging.services.NotificationTemplateService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class InsurerStaffNotificationServiceImpl implements InsurerStaffNotificationService {

    private static final Set<String> CLAIM_STAFF_ROLES = Set.of(
            RoleCodes.PARTNER_ADMIN, RoleCodes.CLAIMS_OFFICER, RoleCodes.CLAIMS_SUPERVISOR);

    private static final Set<ClaimStatus> STAFF_STATUS_ALERTS = Set.of(
            ClaimStatus.SUBMITTED,
            ClaimStatus.UNDER_REVIEW,
            ClaimStatus.INFORMATION_REQUIRED,
            ClaimStatus.APPROVED,
            ClaimStatus.REJECTED,
            ClaimStatus.PAYMENT_PROCESSING,
            ClaimStatus.PAID);

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final NotificationTemplateService notificationTemplateService;
    private final PlatformProperties platformProperties;

    @Override
    public void notifyClaimSubmitted(Claim claim, String claimantName, String policyNumber) {
        Map<String, String> variables = baseClaimVariables(claim, claimantName, policyNumber);
        variables.put("actionLabel", "Review claim");
        notifyPartnerStaff(claim.getOrganizationId(), "INSURER_CLAIM_SUBMITTED", variables);
        sendBrandedEmail(
                claim.getOrganizationId(), "insurer-claim-submitted", variables, "INSURER_CLAIM_SUBMITTED");
    }

    @Override
    public void notifyClaimStatusChange(
            Claim claim, ClaimStatus newStatus, String claimantName, String policyNumber, String reason) {
        if (!STAFF_STATUS_ALERTS.contains(newStatus)) {
            return;
        }
        Map<String, String> variables = baseClaimVariables(claim, claimantName, policyNumber);
        variables.put("status", newStatus.name());
        variables.put("statusLabel", formatStatus(newStatus));
        variables.put("reason", reason != null ? reason : "");
        variables.put("actionLabel", "View claim");
        notifyPartnerStaff(claim.getOrganizationId(), "INSURER_CLAIM_STATUS", variables);
        sendBrandedEmail(claim.getOrganizationId(), "insurer-claim-status", variables, "INSURER_CLAIM_STATUS");
    }

    @Override
    public void notifyClaimDecision(
            Claim claim, String decisionLabel, String claimantName, String policyNumber, String reason) {
        Map<String, String> variables = baseClaimVariables(claim, claimantName, policyNumber);
        variables.put("decision", decisionLabel);
        variables.put("reason", reason != null ? reason : "");
        variables.put("actionLabel", "View claim");
        notifyPartnerStaff(claim.getOrganizationId(), "INSURER_CLAIM_DECISION", variables);
        sendBrandedEmail(claim.getOrganizationId(), "insurer-claim-decision", variables, "INSURER_CLAIM_DECISION");
    }

    @Override
    public void notifyPartnerStaff(UUID organizationId, String templateCode, Map<String, String> variables) {
        for (User staff : findClaimStaff(organizationId)) {
            notificationTemplateService.notifyAllChannels(
                    staff.getId(),
                    organizationId,
                    templateCode,
                    staff.getEmail(),
                    staff.getPhoneNumber(),
                    variables,
                    2,
                    "CLAIM",
                    parseClaimId(variables));
        }
    }

    private void sendBrandedEmail(
            UUID organizationId, String templateName, Map<String, String> variables, String context) {
        for (User staff : findClaimStaff(organizationId)) {
            if (staff.getEmail() == null || staff.getEmail().isBlank()) {
                continue;
            }
            try {
                Map<String, String> emailVars = new HashMap<>(variables);
                emailVars.putIfAbsent("fullName", staff.getFirstName() + " " + staff.getLastName());
                notificationService.sendTemplatedEmail(staff.getEmail(), templateName, emailVars);
            } catch (Exception ex) {
                log.warn("Failed to send {} email to {}: {}", context, staff.getEmail(), ex.getMessage());
            }
        }
    }

    private List<User> findClaimStaff(UUID organizationId) {
        return userRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .filter(user -> user.getStatus() == UserStatus.ACTIVE
                        || user.getStatus() == UserStatus.PENDING_PASSWORD_CHANGE)
                .filter(this::isClaimStaff)
                .toList();
    }

    private boolean isClaimStaff(User user) {
        if (user.getRoles() == null || user.getRoles().isEmpty()) {
            return false;
        }
        return user.getRoles().stream().map(Role::getCode).anyMatch(CLAIM_STAFF_ROLES::contains);
    }

    private Map<String, String> baseClaimVariables(Claim claim, String claimantName, String policyNumber) {
        Map<String, String> variables = new HashMap<>();
        variables.put("claimId", claim.getId().toString());
        variables.put("claimNumber", claim.getClaimNumber());
        variables.put("claimType", claim.getClaimType() != null ? claim.getClaimType() : "Claim");
        variables.put(
                "claimedAmount",
                claim.getClaimedAmount() != null ? claim.getClaimedAmount().toPlainString() : "");
        variables.put("currency", "RWF");
        variables.put("claimantName", claimantName != null ? claimantName : "Policyholder");
        variables.put("policyNumber", policyNumber != null ? policyNumber : "");
        variables.put("claimsUrl", platformProperties.getFrontendBaseUrl() + "/insurer/claims");
        return variables;
    }

    private UUID parseClaimId(Map<String, String> variables) {
        String raw = variables.get("claimId");
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String formatStatus(ClaimStatus status) {
        return switch (status) {
            case SUBMITTED -> "Submitted";
            case UNDER_REVIEW -> "Under review";
            case INFORMATION_REQUIRED -> "Information requested";
            case APPROVED -> "Approved";
            case REJECTED -> "Rejected";
            case PAYMENT_PROCESSING -> "Payment processing";
            case PAID -> "Paid";
            default -> status.name();
        };
    }
}
