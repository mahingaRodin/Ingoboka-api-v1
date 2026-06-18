package com.ingoboka_api.v1.billing.repositories;

import com.ingoboka_api.v1.billing.models.PremiumSchedule;
import com.ingoboka_api.v1.common.enums.PremiumScheduleStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PremiumScheduleRepository extends JpaRepository<PremiumSchedule, UUID> {

    List<PremiumSchedule> findByPolicyIdOrderByDueDateAsc(UUID policyId);

    Optional<PremiumSchedule> findFirstByPolicyIdAndStatusOrderByDueDateAsc(
            UUID policyId, PremiumScheduleStatus status);

    List<PremiumSchedule> findByStatusAndDueDateLessThanEqual(PremiumScheduleStatus status, LocalDate dueDate);

    List<PremiumSchedule> findByStatusAndDueDateBefore(PremiumScheduleStatus status, LocalDate dueDate);

    boolean existsByPolicyIdAndStatus(UUID policyId, PremiumScheduleStatus status);
}
