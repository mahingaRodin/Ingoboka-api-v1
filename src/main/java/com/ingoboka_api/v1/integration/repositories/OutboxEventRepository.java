package com.ingoboka_api.v1.integration.repositories;

import com.ingoboka_api.v1.integration.models.OutboxEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {}
