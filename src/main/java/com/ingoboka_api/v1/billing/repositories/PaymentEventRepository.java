package com.ingoboka_api.v1.billing.repositories;

import com.ingoboka_api.v1.billing.models.PaymentEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentEventRepository extends JpaRepository<PaymentEvent, UUID> {

    Optional<PaymentEvent> findByIdempotencyKey(String idempotencyKey);
}
