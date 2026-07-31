package com.ingoboka_api.v1.messaging.impls;

import com.ingoboka_api.v1.messaging.services.SmsDeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

/**
 * Dev/sandbox fallback: SMS is written to API logs only.
 * Active when Africa's Talking and MTN bulk SMS are both disabled.
 */
@Slf4j
@Service
@ConditionalOnExpression(
        "'${ingoboka.sms.africastalking.enabled:false}' != 'true'"
                + " && '${ingoboka.sms.mtn-bulk.enabled:false}' != 'true'")
public class LogSmsDeliveryServiceImpl implements SmsDeliveryService {

    @Override
    public void send(String phoneNumber, String message) {
        log.info("SMS [{}]: {}", phoneNumber, message);
    }
}
