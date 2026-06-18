package com.ingoboka_api.v1.enrollment.repositories;

import com.ingoboka_api.v1.common.enums.ApplicationStatus;
import com.ingoboka_api.v1.enrollment.models.PolicyApplication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyApplicationRepository extends JpaRepository<PolicyApplication, UUID> {

    List<PolicyApplication> findByCitizenProfileIdOrderBySubmittedAtDesc(UUID citizenProfileId);

    Page<PolicyApplication> findByCitizenProfileIdOrderBySubmittedAtDesc(
            UUID citizenProfileId, Pageable pageable);

    List<PolicyApplication> findByOrganizationIdOrderBySubmittedAtDesc(UUID organizationId);

    Page<PolicyApplication> findByOrganizationIdOrderBySubmittedAtDesc(UUID organizationId, Pageable pageable);

    List<PolicyApplication> findByOrganizationIdAndStatusOrderBySubmittedAtDesc(
            UUID organizationId, ApplicationStatus status);

    Page<PolicyApplication> findByOrganizationIdAndStatusOrderBySubmittedAtDesc(
            UUID organizationId, ApplicationStatus status, Pageable pageable);

    Optional<PolicyApplication> findByApplicationReference(String applicationReference);

    long countByOrganizationIdAndStatus(UUID organizationId, ApplicationStatus status);
}
