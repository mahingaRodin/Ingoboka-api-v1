package com.ingoboka_api.v1.billing.repositories;

import com.ingoboka_api.v1.billing.models.Receipt;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptRepository extends JpaRepository<Receipt, UUID> {

    Page<Receipt> findByPolicyIdOrderByIssuedAtDesc(UUID policyId, Pageable pageable);

    Page<Receipt> findByOrganizationIdOrderByIssuedAtDesc(UUID organizationId, Pageable pageable);
}
