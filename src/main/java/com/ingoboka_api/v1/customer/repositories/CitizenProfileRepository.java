package com.ingoboka_api.v1.customer.repositories;

import com.ingoboka_api.v1.customer.models.CitizenProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitizenProfileRepository extends JpaRepository<CitizenProfile, UUID> {

    Optional<CitizenProfile> findByUserId(UUID userId);

    boolean existsByNationalIdHash(String nationalIdHash);
}
