package com.ingoboka_api.v1.document.repositories;

import com.ingoboka_api.v1.common.enums.DocumentEntityType;
import com.ingoboka_api.v1.document.models.DocumentRegistry;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRegistryRepository extends JpaRepository<DocumentRegistry, UUID> {

    Page<DocumentRegistry> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            DocumentEntityType entityType, UUID entityId, Pageable pageable);

    Page<DocumentRegistry> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);
}
