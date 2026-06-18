package com.ingoboka_api.v1.messaging.services;

import com.ingoboka_api.v1.common.enums.NotificationChannel;
import java.util.Map;
import java.util.UUID;

public interface NotificationTemplateService {

    void sendTemplated(
            UUID userId,
            UUID organizationId,
            String templateCode,
            NotificationChannel channel,
            String recipientEmail,
            Map<String, String> variables);
}
