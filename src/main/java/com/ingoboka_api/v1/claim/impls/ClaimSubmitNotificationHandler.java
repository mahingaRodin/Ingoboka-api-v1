package com.ingoboka_api.v1.claim.impls;

import com.ingoboka_api.v1.claim.models.Claim;
import com.ingoboka_api.v1.claim.repositories.ClaimRepository;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.messaging.services.InsurerStaffNotificationService;
import com.ingoboka_api.v1.messaging.services.NotificationTemplateService;
import com.ingoboka_api.v1.policy.models.Policy;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClaimSubmitNotificationHandler {

    private final ClaimRepository claimRepository;
    private final PolicyRepository policyRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final UserRepository userRepository;
    private final NotificationTemplateService notificationTemplateService;
    private final InsurerStaffNotificationService insurerStaffNotificationService;

    @Transactional(readOnly = true)
    public void sendSubmitNotifications(UUID claimId) {
        Claim claim = claimRepository.findById(claimId).orElse(null);
        if (claim == null) {
            log.warn("Claim {} not found for submit notifications", claimId);
            return;
        }

        notifyClaimholder(claim);
        ClaimContext ctx = resolveClaimContext(claim);
        insurerStaffNotificationService.notifyClaimSubmitted(claim, ctx.claimantName(), ctx.policyNumber());
    }

    private void notifyClaimholder(Claim claim) {
        citizenProfileRepository.findById(claim.getCitizenProfileId()).ifPresent(profile -> userRepository
                .findById(profile.getUserId())
                .ifPresent(user -> notificationTemplateService.notifyAllChannels(
                        user.getId(),
                        claim.getOrganizationId(),
                        "CLAIM_SUBMITTED",
                        user.getEmail(),
                        user.getPhoneNumber(),
                        Map.of(
                                "claimNumber", claim.getClaimNumber(),
                                "decision", "SUBMITTED",
                                "notes", "Your claim is now under review."),
                        1,
                        "CLAIM",
                        claim.getId())));
    }

    private ClaimContext resolveClaimContext(Claim claim) {
        String policyNumber = policyRepository
                .findById(claim.getPolicyId())
                .map(Policy::getPolicyNumber)
                .orElse("");
        String claimantName = citizenProfileRepository
                .findById(claim.getCitizenProfileId())
                .flatMap(profile -> userRepository.findById(profile.getUserId()))
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse("Policyholder");
        return new ClaimContext(claimantName, policyNumber);
    }

    private record ClaimContext(String claimantName, String policyNumber) {}
}
