package com.ingoboka_api.v1.product.repositories;

import com.ingoboka_api.v1.product.models.EligibilityRule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EligibilityRuleRepository extends JpaRepository<EligibilityRule, UUID> {

    List<EligibilityRule> findByPlanId(UUID planId);
}
