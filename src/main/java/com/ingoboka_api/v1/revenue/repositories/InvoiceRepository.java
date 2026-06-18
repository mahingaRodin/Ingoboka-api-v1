package com.ingoboka_api.v1.revenue.repositories;

import com.ingoboka_api.v1.revenue.models.Invoice;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Page<Invoice> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);
}
