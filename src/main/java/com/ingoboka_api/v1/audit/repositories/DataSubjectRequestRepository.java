package com.ingoboka_api.v1.audit.repositories;

import com.ingoboka_api.v1.audit.models.DataSubjectRequest;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataSubjectRequestRepository extends JpaRepository<DataSubjectRequest, UUID> {

    Page<DataSubjectRequest> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<DataSubjectRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
