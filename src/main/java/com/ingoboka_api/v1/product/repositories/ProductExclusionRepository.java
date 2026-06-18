package com.ingoboka_api.v1.product.repositories;

import com.ingoboka_api.v1.product.models.ProductExclusion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductExclusionRepository extends JpaRepository<ProductExclusion, UUID> {

    List<ProductExclusion> findByPlanIdOrderBySortOrderAsc(UUID planId);
}
