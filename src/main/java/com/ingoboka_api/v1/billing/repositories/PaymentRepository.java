package com.ingoboka_api.v1.billing.repositories;

import com.ingoboka_api.v1.billing.models.Payment;
import com.ingoboka_api.v1.common.enums.PaymentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByProviderReference(String providerReference);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByCitizenProfileIdOrderByInitiatedAtDesc(UUID citizenProfileId);

    List<Payment> findByPolicyIdOrderByInitiatedAtDesc(UUID policyId);

    boolean existsByPolicyIdAndStatus(UUID policyId, PaymentStatus status);
}
