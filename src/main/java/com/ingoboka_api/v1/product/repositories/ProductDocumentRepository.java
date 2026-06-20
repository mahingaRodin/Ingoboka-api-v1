package com.ingoboka_api.v1.product.repositories;

import com.ingoboka_api.v1.product.models.ProductDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductDocumentRepository extends JpaRepository<ProductDocument, UUID> {

    List<ProductDocument> findByProductIdOrderBySortOrderAsc(UUID productId);
}
