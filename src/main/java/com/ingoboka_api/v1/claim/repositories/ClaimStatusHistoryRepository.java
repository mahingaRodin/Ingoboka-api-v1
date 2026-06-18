package com.ingoboka_api.v1.claim.repositories;

import com.ingoboka_api.v1.claim.models.ClaimStatusHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClaimStatusHistoryRepository extends JpaRepository<ClaimStatusHistory, UUID> {

    List<ClaimStatusHistory> findByClaimIdOrderByCreatedAtAsc(UUID claimId);
}
