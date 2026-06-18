package com.ingoboka_api.v1.billing.repositories;

import com.ingoboka_api.v1.billing.models.Payment;
import com.ingoboka_api.v1.common.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByProviderReference(String providerReference);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findByCitizenProfileIdOrderByInitiatedAtDesc(UUID citizenProfileId);

    Page<Payment> findByCitizenProfileIdOrderByInitiatedAtDesc(UUID citizenProfileId, Pageable pageable);

    List<Payment> findByPolicyIdOrderByInitiatedAtDesc(UUID policyId);

    Page<Payment> findByPolicyIdOrderByInitiatedAtDesc(UUID policyId, Pageable pageable);

    List<Payment> findByOrganizationIdOrderByInitiatedAtDesc(UUID organizationId);

    Page<Payment> findByOrganizationIdOrderByInitiatedAtDesc(UUID organizationId, Pageable pageable);

    boolean existsByPolicyIdAndStatus(UUID policyId, PaymentStatus status);

    long countByOrganizationIdAndStatus(UUID organizationId, PaymentStatus status);

    @Query(
            value = "SELECT COALESCE(SUM(p.amount), 0) FROM payments p "
                    + "WHERE p.organization_id = :orgId AND p.status = 'SUCCESS' "
                    + "AND CAST(p.completed_at AS date) = :date",
            nativeQuery = true)
    BigDecimal sumSuccessfulByOrganizationAndDate(
            @Param("orgId") UUID organizationId, @Param("date") LocalDate date);
}
