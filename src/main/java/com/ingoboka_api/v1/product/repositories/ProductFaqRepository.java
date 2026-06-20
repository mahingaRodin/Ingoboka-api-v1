package com.ingoboka_api.v1.product.repositories;

import com.ingoboka_api.v1.product.models.ProductFaq;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductFaqRepository extends JpaRepository<ProductFaq, UUID> {

    List<ProductFaq> findByProductIdOrderBySortOrderAsc(UUID productId);
}
