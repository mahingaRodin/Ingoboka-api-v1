package com.ingoboka_api.v1.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ingoboka.policy.lifecycle")
public class PolicyLifecycleProperties {
    private int gracePeriodDays = 30;
    private int billReminderDaysBeforeDue = 7;
}
