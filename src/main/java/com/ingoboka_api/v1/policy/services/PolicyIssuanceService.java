package com.ingoboka_api.v1.policy.services;

import com.ingoboka_api.v1.common.responses.PolicyResponse;
import com.ingoboka_api.v1.common.responses.PolicyVerificationResponse;
import com.ingoboka_api.v1.enrollment.models.PolicyApplication;
import com.ingoboka_api.v1.policy.models.Policy;
import java.util.List;
import java.util.UUID;

public interface PolicyIssuanceService {

    PolicyResponse issueFromApprovedApplication(PolicyApplication application);

    void activatePolicy(UUID policyId);
}
