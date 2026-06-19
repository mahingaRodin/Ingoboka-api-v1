package com.ingoboka_api.v1.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ingoboka.sms.mtn-bulk")
public class MtnBulkSmsProperties {

    /** When false, SMS is logged only (dev/sandbox). */
    private boolean enabled = false;

    private String apiUrl = "https://bulk.mtn.co.rw/api/send";

    private String apiKey = "";

    private String senderId = "INGOBOKA";
}
