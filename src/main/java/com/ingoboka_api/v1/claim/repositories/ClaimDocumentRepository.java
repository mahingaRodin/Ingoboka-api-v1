package com.ingoboka_api.v1.claim.repositories;

import com.ingoboka_api.v1.claim.models.ClaimDocument;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, UUID> {

    List<ClaimDocument> findByClaimIdOrderByCreatedAtAsc(UUID claimId);
}
