package com.ingoboka_api.v1.messaging.repositories;

import com.ingoboka_api.v1.common.enums.NotificationChannel;
import com.ingoboka_api.v1.messaging.models.UserNotification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {

    Page<UserNotification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<UserNotification> findByUserIdAndChannelOrderByCreatedAtDesc(
            UUID userId, NotificationChannel channel, Pageable pageable);

    long countByUserIdAndReadAtIsNullAndChannel(UUID userId, NotificationChannel channel);

    long countByUserIdAndReadAtIsNullAndChannelAndPriorityGreaterThanEqual(
            UUID userId, NotificationChannel channel, int priority);

    @Modifying
    @Query(
            """
            UPDATE UserNotification n
            SET n.status = com.ingoboka_api.v1.common.enums.NotificationDeliveryStatus.READ,
                n.readAt = :readAt
            WHERE n.userId = :userId AND n.readAt IS NULL
            """)
    int markAllRead(@Param("userId") UUID userId, @Param("readAt") Instant readAt);

    @Modifying
    @Query("DELETE FROM UserNotification n WHERE n.userId = :userId AND n.readAt IS NOT NULL")
    int deleteReadByUserId(@Param("userId") UUID userId);
}
