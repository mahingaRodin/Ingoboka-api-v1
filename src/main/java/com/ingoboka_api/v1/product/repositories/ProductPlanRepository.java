package com.ingoboka_api.v1.product.repositories;

import com.ingoboka_api.v1.common.enums.ProductStatus;
import com.ingoboka_api.v1.product.models.ProductPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductPlanRepository extends JpaRepository<ProductPlan, UUID> {

    List<ProductPlan> findByProductIdOrderByCreatedAtAsc(UUID productId);

    Page<ProductPlan> findByProductIdOrderByCreatedAtDesc(UUID productId, Pageable pageable);

    Page<ProductPlan> findByProductIdAndStatusOrderByCreatedAtDesc(
            UUID productId, ProductStatus status, Pageable pageable);

    Optional<ProductPlan> findByIdAndProductId(UUID id, UUID productId);

    boolean existsByProductIdAndCode(UUID productId, String code);

    List<ProductPlan> findByProductIdAndStatus(UUID productId, ProductStatus status);
}
