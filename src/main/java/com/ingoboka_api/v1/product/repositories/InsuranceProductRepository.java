package com.ingoboka_api.v1.product.repositories;

import com.ingoboka_api.v1.common.enums.ProductStatus;
import com.ingoboka_api.v1.product.models.InsuranceProduct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, UUID> {

    List<InsuranceProduct> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Page<InsuranceProduct> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    List<InsuranceProduct> findByStatusOrderByPublishedAtDesc(ProductStatus status);

    Page<InsuranceProduct> findByStatusOrderByPublishedAtDesc(ProductStatus status, Pageable pageable);

    Optional<InsuranceProduct> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);
}
