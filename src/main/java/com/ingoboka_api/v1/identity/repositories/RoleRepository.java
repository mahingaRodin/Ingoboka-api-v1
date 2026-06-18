package com.ingoboka_api.v1.identity.repositories;

import com.ingoboka_api.v1.identity.models.Role;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByCode(String code);
}
