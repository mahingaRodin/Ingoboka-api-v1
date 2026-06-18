package com.ingoboka_api.v1.integration.impls;

import com.ingoboka_api.v1.common.enums.OutboxEventStatus;
import com.ingoboka_api.v1.integration.models.OutboxEvent;
import com.ingoboka_api.v1.integration.repositories.OutboxEventRepository;
import com.ingoboka_api.v1.integration.services.OutboxPublisherService;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisherServiceImpl implements OutboxPublisherService {

    private final OutboxEventRepository outboxEventRepository;

    @Override
    @Transactional
    public void publish(String aggregateType, UUID aggregateId, String eventType, String payload) {
        Instant now = Instant.now();
        OutboxEvent event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setEventType(eventType);
        event.setPayload(payload);
        event.setStatus(OutboxEventStatus.PUBLISHED);
        event.setPublishedAt(now);
        event.setCreatedAt(now);
        outboxEventRepository.save(event);
        log.debug("Outbox event {} recorded for {} {}", eventType, aggregateType, aggregateId);
    }
}
