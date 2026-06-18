package com.ingoboka_api.v1.policy.services;

import com.ingoboka_api.v1.common.requests.AttachPolicyDocumentRequest;
import com.ingoboka_api.v1.common.responses.PolicyResponse;
import com.ingoboka_api.v1.common.responses.PolicyVerificationResponse;
import com.ingoboka_api.v1.policy.models.Policy;
import java.util.List;
import java.util.UUID;

public interface PolicyService {

    PolicyResponse getPolicy(UUID policyId);

    List<PolicyResponse> listMyPolicies();

    List<PolicyResponse> listTenantPolicies();

    PolicyVerificationResponse verifyByQrToken(String token);

    void attachDocument(UUID policyId, AttachPolicyDocumentRequest request);

    Policy requirePolicyForPayment(UUID policyId, UUID citizenProfileId);
}
