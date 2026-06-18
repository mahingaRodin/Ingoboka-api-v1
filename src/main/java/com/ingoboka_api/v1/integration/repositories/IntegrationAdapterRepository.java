package com.ingoboka_api.v1.integration.repositories;

import com.ingoboka_api.v1.integration.models.IntegrationAdapter;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationAdapterRepository extends JpaRepository<IntegrationAdapter, UUID> {

    Optional<IntegrationAdapter> findByCode(String code);

    Page<IntegrationAdapter> findAllByOrderByCodeAsc(Pageable pageable);
}
