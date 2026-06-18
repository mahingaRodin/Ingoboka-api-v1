package com.ingoboka_api.v1.partner.repositories;

import com.ingoboka_api.v1.partner.models.OrganizationSettings;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationSettingsRepository extends JpaRepository<OrganizationSettings, UUID> {

    Optional<OrganizationSettings> findByOrganizationId(UUID organizationId);
}
