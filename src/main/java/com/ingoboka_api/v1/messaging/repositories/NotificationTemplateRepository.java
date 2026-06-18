package com.ingoboka_api.v1.messaging.repositories;

import com.ingoboka_api.v1.messaging.models.NotificationTemplate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByCodeAndActiveTrue(String code);
}
