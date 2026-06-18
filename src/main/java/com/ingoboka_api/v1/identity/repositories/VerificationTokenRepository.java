package com.ingoboka_api.v1.identity.repository;

import com.ingoboka_api.v1.common.enums.VerificationTokenType;
import com.ingoboka_api.v1.identity.domain.VerificationToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

    @EntityGraph(attributePaths = {"user", "user.roles"})
    Optional<VerificationToken> findByTokenHashAndTypeAndConsumedAtIsNull(
            String tokenHash, VerificationTokenType type);
}
