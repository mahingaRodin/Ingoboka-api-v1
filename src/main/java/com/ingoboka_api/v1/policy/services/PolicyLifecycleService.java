package com.ingoboka_api.v1.policy.services;

import com.ingoboka_api.v1.common.responses.PolicyResponse;
import java.util.UUID;

public interface PolicyLifecycleService {

    PolicyResponse renewPolicy(UUID policyId);

    void processDailyLifecycle();
}
