package com.ingoboka_api.v1.identity.repository;

import com.ingoboka_api.v1.identity.domain.Organization;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findByCode(String code);
}
