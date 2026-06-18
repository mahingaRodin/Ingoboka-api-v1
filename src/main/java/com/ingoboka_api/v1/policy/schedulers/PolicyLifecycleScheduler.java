package com.ingoboka_api.v1.policy.schedulers;

import com.ingoboka_api.v1.policy.services.PolicyLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyLifecycleScheduler {

    private final PolicyLifecycleService policyLifecycleService;

    @Scheduled(cron = "${ingoboka.policy.lifecycle.cron:0 0 2 * * *}")
    public void runDailyLifecycle() {
        log.info("Running daily policy lifecycle job");
        policyLifecycleService.processDailyLifecycle();
    }
}
