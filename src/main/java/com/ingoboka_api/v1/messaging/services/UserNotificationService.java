package com.ingoboka_api.v1.messaging.services;

import com.ingoboka_api.v1.common.enums.NotificationChannel;
import com.ingoboka_api.v1.common.responses.NotificationSummaryResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.UserNotificationResponse;
import java.util.UUID;

public interface UserNotificationService {

    UserNotificationResponse dispatch(
            UUID userId,
            UUID organizationId,
            NotificationChannel channel,
            String templateCode,
            String subject,
            String body);

    UserNotificationResponse dispatch(
            UUID userId,
            UUID organizationId,
            NotificationChannel channel,
            String templateCode,
            String subject,
            String body,
            int priority,
            String referenceType,
            UUID referenceId);

    PageResponse<UserNotificationResponse> listMyNotifications(int page, int size, NotificationChannel channel);

    NotificationSummaryResponse getMySummary();

    UserNotificationResponse markRead(UUID notificationId);

    int markAllRead();

    int clearRead();
}
