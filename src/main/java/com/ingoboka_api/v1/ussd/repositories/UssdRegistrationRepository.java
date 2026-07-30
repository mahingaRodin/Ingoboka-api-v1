package com.ingoboka_api.v1.ussd.repositories;

import com.ingoboka_api.v1.ussd.models.UssdRegistration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UssdRegistrationRepository extends JpaRepository<UssdRegistration, UUID> {

    boolean existsByPhoneNumber(String phoneNumber);

    Optional<UssdRegistration> findByPhoneNumber(String phoneNumber);

    boolean existsByReferenceCode(String referenceCode);
}
