package com.ingoboka_api.v1.enrollment.repositories;

import com.ingoboka_api.v1.common.enums.ApplicationStatus;
import com.ingoboka_api.v1.enrollment.models.PolicyApplication;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyApplicationRepository extends JpaRepository<PolicyApplication, UUID> {

    List<PolicyApplication> findByCitizenProfileIdOrderBySubmittedAtDesc(UUID citizenProfileId);

    List<PolicyApplication> findByOrganizationIdOrderBySubmittedAtDesc(UUID organizationId);

    List<PolicyApplication> findByOrganizationIdAndStatusOrderBySubmittedAtDesc(
            UUID organizationId, ApplicationStatus status);

    Optional<PolicyApplication> findByApplicationReference(String applicationReference);
}
