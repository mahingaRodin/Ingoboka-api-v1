package com.ingoboka_api.v1.policy.services;

import com.ingoboka_api.v1.common.requests.AttachPolicyDocumentRequest;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.PolicyActivityResponse;
import com.ingoboka_api.v1.common.responses.PolicyCardResponse;
import com.ingoboka_api.v1.common.responses.PolicyResponse;
import com.ingoboka_api.v1.common.responses.PolicyVerificationResponse;
import com.ingoboka_api.v1.common.responses.PremiumScheduleResponse;
import com.ingoboka_api.v1.policy.models.Policy;
import java.util.UUID;

public interface PolicyService {

    PolicyResponse getPolicy(UUID policyId);

    PageResponse<PolicyResponse> listMyPolicies(int page, int size);

    PageResponse<PolicyResponse> listTenantPolicies(int page, int size);

    PolicyVerificationResponse verifyByQrToken(String token);

    void attachDocument(UUID policyId, AttachPolicyDocumentRequest request);

    Policy requirePolicyForPayment(UUID policyId, UUID citizenProfileId);

    PageResponse<PremiumScheduleResponse> listPremiumSchedules(UUID policyId, int page, int size);

    PolicyCardResponse getPolicyCard(UUID policyId);

    PageResponse<PolicyActivityResponse> listMyActivity(int page, int size);
}
