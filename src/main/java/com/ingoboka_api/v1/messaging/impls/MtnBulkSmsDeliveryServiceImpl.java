package com.ingoboka_api.v1.messaging.impls;

import com.ingoboka_api.v1.common.config.MtnBulkSmsProperties;
import com.ingoboka_api.v1.messaging.services.SmsDeliveryService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ingoboka.sms.mtn-bulk.enabled", havingValue = "true")
public class MtnBulkSmsDeliveryServiceImpl implements SmsDeliveryService {

    private final MtnBulkSmsProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public void send(String phoneNumber, String message) {
        String msisdn = normalizeRwandaMsisdn(phoneNumber);
        if (!StringUtils.hasText(properties.getApiKey())) {
            log.warn("MTN bulk SMS enabled but api-key missing; logging message to {}", msisdn);
            log.info("SMS [{}]: {}", msisdn, message);
            return;
        }

        try {
            RestClient client = restClientBuilder.build();
            client.post()
                    .uri(properties.getApiUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .body(Map.of(
                            "sender", properties.getSenderId(),
                            "message", message,
                            "recipients", msisdn))
                    .retrieve()
                    .toBodilessEntity();
            log.info("MTN bulk SMS queued to {}", msisdn);
        } catch (Exception ex) {
            log.error("MTN bulk SMS failed for {}: {}", msisdn, ex.getMessage());
            log.info("SMS fallback log [{}]: {}", msisdn, message);
        }
    }

    private String normalizeRwandaMsisdn(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("250")) {
            return digits;
        }
        if (digits.startsWith("0")) {
            return "25" + digits;
        }
        if (digits.length() == 9) {
            return "250" + digits;
        }
        return digits;
    }
}
