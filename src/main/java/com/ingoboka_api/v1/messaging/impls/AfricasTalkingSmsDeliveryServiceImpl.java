package com.ingoboka_api.v1.messaging.impls;

import com.ingoboka_api.v1.common.config.AfricasTalkingSmsProperties;
import com.ingoboka_api.v1.messaging.services.SmsDeliveryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ingoboka.sms.africastalking.enabled", havingValue = "true")
public class AfricasTalkingSmsDeliveryServiceImpl implements SmsDeliveryService {

    private final AfricasTalkingSmsProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public void send(String phoneNumber, String message) {
        String to = normalizeE164(phoneNumber);
        if (!StringUtils.hasText(properties.getApiKey())) {
            log.warn("Africa's Talking SMS enabled but api-key missing; logging message to {}", to);
            log.info("SMS [{}]: {}", to, message);
            return;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", properties.getUsername());
        form.add("to", to);
        form.add("message", message);
        if (StringUtils.hasText(properties.getFrom())) {
            form.add("from", properties.getFrom());
        }

        try {
            String body = restClientBuilder
                    .build()
                    .post()
                    .uri(properties.getApiUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("apiKey", properties.getApiKey())
                    .body(form)
                    .retrieve()
                    .body(String.class);
            log.info("Africa's Talking SMS queued to {}: {}", to, body);
        } catch (Exception ex) {
            log.error("Africa's Talking SMS failed for {}: {}", to, ex.getMessage());
            log.info("SMS fallback log [{}]: {}", to, message);
        }
    }

    private static String normalizeE164(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("250")) {
            return "+" + digits;
        }
        if (digits.startsWith("0") && digits.length() == 10) {
            return "+25" + digits;
        }
        if (digits.length() == 9) {
            return "+250" + digits;
        }
        return phone.startsWith("+") ? phone : "+" + digits;
    }
}
