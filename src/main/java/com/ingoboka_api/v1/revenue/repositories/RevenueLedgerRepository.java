package com.ingoboka_api.v1.revenue.repositories;

import com.ingoboka_api.v1.revenue.models.RevenueLedgerEntry;
import com.ingoboka_api.v1.common.enums.RevenueLedgerStatus;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RevenueLedgerRepository extends JpaRepository<RevenueLedgerEntry, UUID> {

    Page<RevenueLedgerEntry> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM RevenueLedgerEntry e "
            + "WHERE e.organizationId = :organizationId AND e.status = :status")
    BigDecimal sumAmountByOrganizationIdAndStatus(
            @Param("organizationId") UUID organizationId, @Param("status") RevenueLedgerStatus status);
}
