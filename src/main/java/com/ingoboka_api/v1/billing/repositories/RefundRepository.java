package com.ingoboka_api.v1.billing.repositories;

import com.ingoboka_api.v1.billing.models.Refund;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefundRepository extends JpaRepository<Refund, UUID> {

    Page<Refund> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId, Pageable pageable);

    @Query(
            value = "SELECT COALESCE(SUM(r.amount), 0) FROM refunds r "
                    + "JOIN payments p ON r.payment_id = p.id "
                    + "WHERE p.organization_id = :orgId AND r.status = 'COMPLETED' "
                    + "AND CAST(r.created_at AS date) = :date",
            nativeQuery = true)
    BigDecimal sumCompletedByOrganizationAndDate(
            @Param("orgId") UUID organizationId, @Param("date") LocalDate date);
}
