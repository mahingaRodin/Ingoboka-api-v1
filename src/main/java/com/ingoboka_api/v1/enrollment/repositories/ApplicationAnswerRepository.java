package com.ingoboka_api.v1.enrollment.repositories;

import com.ingoboka_api.v1.enrollment.models.ApplicationAnswer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationAnswerRepository extends JpaRepository<ApplicationAnswer, UUID> {

    List<ApplicationAnswer> findByApplicationId(UUID applicationId);
}
