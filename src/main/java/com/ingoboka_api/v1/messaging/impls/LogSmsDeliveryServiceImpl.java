package com.ingoboka_api.v1.messaging.impls;

import com.ingoboka_api.v1.messaging.services.SmsDeliveryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "ingoboka.sms.mtn-bulk.enabled", havingValue = "false", matchIfMissing = true)
public class LogSmsDeliveryServiceImpl implements SmsDeliveryService {

    @Override
    public void send(String phoneNumber, String message) {
        log.info("SMS [{}]: {}", phoneNumber, message);
    }
}
