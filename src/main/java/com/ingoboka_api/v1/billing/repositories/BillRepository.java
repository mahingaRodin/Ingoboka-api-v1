package com.ingoboka_api.v1.billing.repositories;

import com.ingoboka_api.v1.billing.models.Bill;
import com.ingoboka_api.v1.common.enums.BillStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BillRepository extends JpaRepository<Bill, UUID> {

    Page<Bill> findByPolicyIdOrderByIssuedAtDesc(UUID policyId, Pageable pageable);

    Page<Bill> findByOrganizationIdOrderByIssuedAtDesc(UUID organizationId, Pageable pageable);

    Optional<Bill> findFirstByPremiumScheduleId(UUID premiumScheduleId);

    List<Bill> findByStatusAndDueDateBefore(BillStatus status, java.time.LocalDate date);
}
