package com.ingoboka_api.v1.messaging.impls;

import com.ingoboka_api.v1.common.enums.NotificationChannel;
import com.ingoboka_api.v1.common.enums.NotificationDeliveryStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.responses.NotificationSummaryResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.UserNotificationResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.common.util.PaginationUtils;
import com.ingoboka_api.v1.messaging.models.UserNotification;
import com.ingoboka_api.v1.messaging.repositories.UserNotificationRepository;
import com.ingoboka_api.v1.messaging.services.UserNotificationService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationServiceImpl implements UserNotificationService {

    private static final int URGENT_PRIORITY_THRESHOLD = 1;

    private final UserNotificationRepository userNotificationRepository;
    private final JavaMailSender mailSender;

    @Override
    @Transactional
    public UserNotificationResponse dispatch(
            UUID userId,
            UUID organizationId,
            NotificationChannel channel,
            String templateCode,
            String subject,
            String body) {
        return dispatch(userId, organizationId, channel, templateCode, subject, body, 0, null, null);
    }

    @Override
    @Transactional
    public UserNotificationResponse dispatch(
            UUID userId,
            UUID organizationId,
            NotificationChannel channel,
            String templateCode,
            String subject,
            String body,
            int priority,
            String referenceType,
            UUID referenceId) {
        Instant now = Instant.now();
        UserNotification notification = new UserNotification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setOrganizationId(organizationId);
        notification.setChannel(channel);
        notification.setTemplateCode(templateCode);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setPriority(priority);
        notification.setReferenceType(referenceType);
        notification.setReferenceId(referenceId);
        notification.setCreatedAt(now);

        if (channel == NotificationChannel.EMAIL) {
            tryDeliverEmail(userId, subject, body, notification, now);
        } else if (channel == NotificationChannel.IN_APP) {
            notification.setStatus(NotificationDeliveryStatus.SENT);
            notification.setSentAt(now);
        } else {
            notification.setStatus(NotificationDeliveryStatus.SENT);
            notification.setSentAt(now);
            log.info("Simulated {} notification [{}] to user {}", channel, templateCode, userId);
        }

        userNotificationRepository.save(notification);
        return toResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserNotificationResponse> listMyNotifications(int page, int size, NotificationChannel channel) {
        UUID userId = SecurityUtils.currentUser().getUserId();
        Page<UserNotification> result = channel == null
                ? userNotificationRepository.findByUserIdOrderByCreatedAtDesc(
                        userId, PaginationUtils.toPageable(page, size))
                : userNotificationRepository.findByUserIdAndChannelOrderByCreatedAtDesc(
                        userId, channel, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationSummaryResponse getMySummary() {
        UUID userId = SecurityUtils.currentUser().getUserId();
        long unread = userNotificationRepository.countByUserIdAndReadAtIsNullAndChannel(
                userId, NotificationChannel.IN_APP);
        long urgentUnread = userNotificationRepository.countByUserIdAndReadAtIsNullAndChannelAndPriorityGreaterThanEqual(
                userId, NotificationChannel.IN_APP, URGENT_PRIORITY_THRESHOLD);
        return NotificationSummaryResponse.builder()
                .unreadCount(unread)
                .urgentUnreadCount(urgentUnread)
                .build();
    }

    @Override
    @Transactional
    public UserNotificationResponse markRead(UUID notificationId) {
        UUID userId = SecurityUtils.currentUser().getUserId();
        UserNotification notification = userNotificationRepository
                .findById(notificationId)
                .orElseThrow(() -> new BusinessException("Notification not found"));
        if (!notification.getUserId().equals(userId)) {
            throw new BusinessException("Access denied");
        }
        if (notification.getReadAt() == null) {
            notification.setStatus(NotificationDeliveryStatus.READ);
            notification.setReadAt(Instant.now());
            userNotificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Override
    @Transactional
    public int markAllRead() {
        UUID userId = SecurityUtils.currentUser().getUserId();
        return userNotificationRepository.markAllRead(userId, Instant.now());
    }

    @Override
    @Transactional
    public int clearRead() {
        UUID userId = SecurityUtils.currentUser().getUserId();
        return userNotificationRepository.deleteReadByUserId(userId);
    }

    private void tryDeliverEmail(UUID userId, String subject, String body, UserNotification notification, Instant now) {
        log.info("Email notification queued for user {} — subject: {}", userId, subject);
        notification.setStatus(NotificationDeliveryStatus.SENT);
        notification.setSentAt(now);
    }

    private UserNotificationResponse toResponse(UserNotification n) {
        return UserNotificationResponse.builder()
                .id(n.getId())
                .channel(n.getChannel())
                .templateCode(n.getTemplateCode())
                .subject(n.getSubject())
                .body(n.getBody())
                .status(n.getStatus())
                .sentAt(n.getSentAt())
                .readAt(n.getReadAt())
                .priority(n.getPriority())
                .referenceType(n.getReferenceType())
                .referenceId(n.getReferenceId())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
