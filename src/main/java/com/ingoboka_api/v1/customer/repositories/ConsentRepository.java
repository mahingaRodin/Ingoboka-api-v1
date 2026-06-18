package com.ingoboka_api.v1.customer.repositories;

import com.ingoboka_api.v1.common.enums.ConsentType;
import com.ingoboka_api.v1.customer.models.Consent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRepository extends JpaRepository<Consent, UUID> {

    List<Consent> findByUserIdOrderByGrantedAtDesc(UUID userId);

    Optional<Consent> findByIdAndUserId(UUID id, UUID userId);

    Optional<Consent> findByUserIdAndConsentTypeAndGrantedTrueAndRevokedAtIsNull(
            UUID userId, ConsentType consentType);
}
