package com.ingoboka_api.v1.integration.services;

import java.util.UUID;

public interface OutboxPublisherService {

    void publish(String aggregateType, UUID aggregateId, String eventType, String payload);
}
