package com.ingoboka_api.v1.revenue.repositories;

import com.ingoboka_api.v1.revenue.models.ContractPriceRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContractPriceRuleRepository extends JpaRepository<ContractPriceRule, UUID> {

    Page<ContractPriceRule> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    List<ContractPriceRule> findByOrganizationIdAndActiveTrue(UUID organizationId);
}
