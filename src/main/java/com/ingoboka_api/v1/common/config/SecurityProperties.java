package com.ingoboka_api.v1.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ingoboka.security")
public class SecurityProperties {

    private long verificationTokenExpirationHours = 24;
    private long passwordResetTokenExpirationHours = 1;
    private long activationTokenExpirationHours = 72;
}
