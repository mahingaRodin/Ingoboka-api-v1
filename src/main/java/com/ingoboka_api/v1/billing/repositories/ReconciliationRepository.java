package com.ingoboka_api.v1.billing.repositories;

import com.ingoboka_api.v1.billing.models.ReconciliationRecord;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationRepository extends JpaRepository<ReconciliationRecord, UUID> {

    Page<ReconciliationRecord> findByOrganizationIdOrderByReconciliationDateDesc(
            UUID organizationId, Pageable pageable);
}
