package com.ingoboka_api.v1.product.repositories;

import com.ingoboka_api.v1.product.models.ProductBenefit;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductBenefitRepository extends JpaRepository<ProductBenefit, UUID> {

    List<ProductBenefit> findByPlanIdOrderBySortOrderAsc(UUID planId);
}
