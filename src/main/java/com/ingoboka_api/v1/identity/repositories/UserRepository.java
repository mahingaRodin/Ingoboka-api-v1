package com.ingoboka_api.v1.identity.repositories;

import com.ingoboka_api.v1.identity.models.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "organization"})
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    List<User> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<User> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
