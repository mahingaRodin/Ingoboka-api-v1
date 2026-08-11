package com.ingoboka_api.v1.messaging.services;

import com.ingoboka_api.v1.claim.models.Claim;
import com.ingoboka_api.v1.common.enums.ClaimStatus;
import java.util.Map;
import java.util.UUID;

public interface InsurerStaffNotificationService {

    void notifyClaimSubmitted(Claim claim, String claimantName, String policyNumber);

    void notifyClaimStatusChange(
            Claim claim, ClaimStatus newStatus, String claimantName, String policyNumber, String reason);

    void notifyClaimDecision(
            Claim claim, String decisionLabel, String claimantName, String policyNumber, String reason);

    void notifyPartnerStaff(UUID organizationId, String templateCode, Map<String, String> variables);
}
