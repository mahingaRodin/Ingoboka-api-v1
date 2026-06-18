package com.ingoboka_api.v1.identity.repositories;

import com.ingoboka_api.v1.common.enums.OrganizationType;
import com.ingoboka_api.v1.identity.models.Organization;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {

    Optional<Organization> findByCode(String code);

    boolean existsByCode(String code);

    List<Organization> findByTypeInOrderByCreatedAtDesc(List<OrganizationType> types);

    Page<Organization> findByTypeInOrderByCreatedAtDesc(List<OrganizationType> types, Pageable pageable);
}
