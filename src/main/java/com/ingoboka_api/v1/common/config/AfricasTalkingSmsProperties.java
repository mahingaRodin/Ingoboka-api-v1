package com.ingoboka_api.v1.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ingoboka.sms.africastalking")
public class AfricasTalkingSmsProperties {

    /** When true, SMS is sent via Africa's Talking (sandbox or live). */
    private boolean enabled = false;

    private String username = "sandbox";

    private String apiKey = "";

    /**
     * Sandbox: https://api.sandbox.africastalking.com/version1/messaging
     * Live: https://api.africastalking.com/version1/messaging
     */
    private String apiUrl = "https://api.sandbox.africastalking.com/version1/messaging";

    /** Optional shortcode / alphanumeric sender (live only in most markets). */
    private String from = "";
}
