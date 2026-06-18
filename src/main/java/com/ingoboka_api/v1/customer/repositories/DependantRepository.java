package com.ingoboka_api.v1.customer.repositories;

import com.ingoboka_api.v1.customer.models.Dependant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DependantRepository extends JpaRepository<Dependant, UUID> {

    List<Dependant> findByCitizenProfileIdOrderByCreatedAtAsc(UUID citizenProfileId);
}
