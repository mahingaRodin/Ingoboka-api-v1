package com.ingoboka_api.v1.partner.repositories;

import com.ingoboka_api.v1.partner.models.PartnerProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartnerProfileRepository extends JpaRepository<PartnerProfile, UUID> {

    Optional<PartnerProfile> findByOrganizationId(UUID organizationId);

    List<PartnerProfile> findByOrganizationIdIn(List<UUID> organizationIds);
}
