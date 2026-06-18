package com.ingoboka_api.v1.partner.repositories;

import com.ingoboka_api.v1.common.enums.ContractStatus;
import com.ingoboka_api.v1.partner.models.PartnerContract;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerContractRepository extends JpaRepository<PartnerContract, UUID> {

    List<PartnerContract> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Page<PartnerContract> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    Optional<PartnerContract> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByContractReference(String contractReference);
}
