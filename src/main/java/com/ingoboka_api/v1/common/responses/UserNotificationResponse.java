package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.NotificationChannel;
import com.ingoboka_api.v1.common.enums.NotificationDeliveryStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserNotificationResponse {
    UUID id;
    NotificationChannel channel;
    String templateCode;
    String subject;
    String body;
    NotificationDeliveryStatus status;
    Instant sentAt;
    Instant readAt;
    Instant createdAt;
}
