package com.ingoboka_api.v1.audit.impls;

import com.ingoboka_api.v1.audit.models.AuditLog;
import com.ingoboka_api.v1.audit.models.DataSubjectRequest;
import com.ingoboka_api.v1.audit.repositories.AuditLogRepository;
import com.ingoboka_api.v1.audit.repositories.DataSubjectRequestRepository;
import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import com.ingoboka_api.v1.common.enums.AuditOutcome;
import com.ingoboka_api.v1.common.enums.DataSubjectRequestStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.ResolveDataSubjectRequest;
import com.ingoboka_api.v1.common.requests.SubmitDataSubjectRequest;
import com.ingoboka_api.v1.common.responses.AuditLogResponse;
import com.ingoboka_api.v1.common.responses.DataSubjectRequestResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.common.util.PaginationUtils;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuditComplianceServiceImpl implements AuditComplianceService {

    private static final Set<String> SORTABLE =
            Set.of("createdAt", "action", "actorEmail", "entityType", "outcome");

    private final AuditLogRepository auditLogRepository;
    private final DataSubjectRequestRepository dataSubjectRequestRepository;

    @Override
    @Transactional
    public void log(String action, String entityType, UUID entityId, String summary) {
        log(action, entityType, entityId, summary, AuditOutcome.SUCCESS.value());
    }

    @Override
    @Transactional
    public void log(String action, String entityType, UUID entityId, String summary, String outcome) {
        persist(action, entityType, entityId, summary, AuditOutcome.from(outcome), safeCurrentUser());
    }

    @Override
    @Transactional
    public void log(AuditOutcome outcome, String action, String entityType, UUID entityId, String summary) {
        persist(action, entityType, entityId, summary, outcome, safeCurrentUser());
    }

    @Override
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void logSystem(
            AuditOutcome outcome,
            String action,
            String entityType,
            UUID entityId,
            String summary,
            String actorEmail) {
        persist(action, entityType, entityId, summary, outcome, null, actorEmail);
    }

    private void persist(
            String action,
            String entityType,
            UUID entityId,
            String summary,
            AuditOutcome outcome,
            IngobokaUserDetails actor) {
        persist(action, entityType, entityId, summary, outcome, actor, actor != null ? actor.getEmail() : "system");
    }

    private void persist(
            String action,
            String entityType,
            UUID entityId,
            String summary,
            AuditOutcome outcome,
            IngobokaUserDetails actor,
            String actorEmail) {
        AuditLog entry = new AuditLog();
        entry.setId(UUID.randomUUID());
        entry.setOrganizationId(actor != null ? actor.getOrganizationId() : null);
        entry.setActorUserId(actor != null ? actor.getUserId() : null);
        entry.setActorEmail(StringUtils.hasText(actorEmail) ? actorEmail : "system");
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setCorrelationId(MDC.get("correlationId"));
        entry.setSummary(summary != null ? summary : action);
        entry.setOutcome(outcome.value());
        entry.setCreatedAt(Instant.now());
        auditLogRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> listAuditLogs(int page, int size) {
        return listAuditLogs(page, size, null, null, null, null, null, null, null, "createdAt", "desc");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> listAuditLogs(
            int page,
            int size,
            String action,
            String actor,
            String resourceType,
            String outcome,
            String search,
            String from,
            String to,
            String sortBy,
            String sortDir) {
        if (!SecurityUtils.currentUser().hasRole(RoleCodes.PLATFORM_ADMIN)) {
            throw new BusinessException("Only platform administrators can view all audit logs");
        }
        Pageable pageable = toSortedPageable(page, size, sortBy, sortDir);
        Specification<AuditLog> spec = buildSpec(null, action, actor, resourceType, outcome, search, from, to);
        Page<AuditLog> result = auditLogRepository.findAll(spec, pageable);
        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> listTenantAuditLogs(int page, int size) {
        return listTenantAuditLogs(page, size, null, null, null, null, null, null, null, "createdAt", "desc");
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> listTenantAuditLogs(
            int page,
            int size,
            String action,
            String actor,
            String resourceType,
            String outcome,
            String search,
            String from,
            String to,
            String sortBy,
            String sortDir) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (!user.hasRole(RoleCodes.COMPLIANCE_AUDITOR)
                && !user.hasRole(RoleCodes.PARTNER_ADMIN)
                && !user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            throw new BusinessException("Access denied");
        }
        if (user.getOrganizationId() == null) {
            throw new BusinessException("No organization associated with this account");
        }
        Pageable pageable = toSortedPageable(page, size, sortBy, sortDir);
        Specification<AuditLog> spec =
                buildSpec(user.getOrganizationId(), action, actor, resourceType, outcome, search, from, to);
        Page<AuditLog> result = auditLogRepository.findAll(spec, pageable);
        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    @Transactional
    public DataSubjectRequestResponse submitDataSubjectRequest(SubmitDataSubjectRequest request) {
        UUID userId = SecurityUtils.currentUser().getUserId();
        Instant now = Instant.now();
        DataSubjectRequest entry = new DataSubjectRequest();
        entry.setId(UUID.randomUUID());
        entry.setUserId(userId);
        entry.setRequestType(request.getRequestType());
        entry.setDetails(request.getDetails());
        entry.setStatus(DataSubjectRequestStatus.SUBMITTED);
        entry.setCreatedAt(now);
        dataSubjectRequestRepository.save(entry);
        log("DATA_SUBJECT_REQUEST_SUBMITTED", "DATA_SUBJECT_REQUEST", entry.getId(), request.getRequestType().name());
        return toDataSubjectResponse(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DataSubjectRequestResponse> listMyDataSubjectRequests(int page, int size) {
        UUID userId = SecurityUtils.currentUser().getUserId();
        Page<DataSubjectRequest> result = dataSubjectRequestRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toDataSubjectResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DataSubjectRequestResponse> listAllDataSubjectRequests(int page, int size) {
        if (!SecurityUtils.currentUser().hasRole(RoleCodes.COMPLIANCE_AUDITOR)
                && !SecurityUtils.currentUser().hasRole(RoleCodes.PLATFORM_ADMIN)) {
            throw new BusinessException("Access denied");
        }
        Page<DataSubjectRequest> result = dataSubjectRequestRepository.findAllByOrderByCreatedAtDesc(
                PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toDataSubjectResponse));
    }

    @Override
    @Transactional
    public DataSubjectRequestResponse resolveDataSubjectRequest(ResolveDataSubjectRequest request) {
        if (!SecurityUtils.currentUser().hasRole(RoleCodes.COMPLIANCE_AUDITOR)
                && !SecurityUtils.currentUser().hasRole(RoleCodes.PLATFORM_ADMIN)) {
            throw new BusinessException("Access denied");
        }
        DataSubjectRequest entry = dataSubjectRequestRepository
                .findById(request.getRequestId())
                .orElseThrow(() -> new BusinessException("Request not found"));
        entry.setStatus(request.getStatus());
        entry.setResolutionNotes(request.getResolutionNotes());
        if (request.getStatus() == DataSubjectRequestStatus.COMPLETED
                || request.getStatus() == DataSubjectRequestStatus.REJECTED) {
            entry.setResolvedAt(Instant.now());
        }
        dataSubjectRequestRepository.save(entry);
        AuditOutcome outcome = request.getStatus() == DataSubjectRequestStatus.REJECTED
                ? AuditOutcome.FAILED
                : AuditOutcome.SUCCESS;
        log(outcome, "DATA_SUBJECT_REQUEST_RESOLVED", "DATA_SUBJECT_REQUEST", entry.getId(), request.getStatus().name());
        return toDataSubjectResponse(entry);
    }

    private Specification<AuditLog> buildSpec(
            UUID organizationId,
            String action,
            String actor,
            String resourceType,
            String outcome,
            String search,
            String from,
            String to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (organizationId != null) {
                predicates.add(cb.equal(root.get("organizationId"), organizationId));
            }
            if (StringUtils.hasText(action)) {
                predicates.add(cb.like(cb.lower(root.get("action")), "%" + action.toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(actor)) {
                predicates.add(
                        cb.like(cb.lower(root.get("actorEmail")), "%" + actor.toLowerCase(Locale.ROOT) + "%"));
            }
            if (StringUtils.hasText(resourceType)) {
                predicates.add(cb.equal(cb.upper(root.get("entityType")), resourceType.toUpperCase(Locale.ROOT)));
            }
            if (StringUtils.hasText(outcome)) {
                String normalized = AuditOutcome.from(outcome).value();
                predicates.add(cb.equal(cb.upper(root.get("outcome")), normalized));
            }
            Instant fromInstant = parseInstant(from);
            Instant toInstant = parseInstant(to);
            if (fromInstant != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
            }
            if (toInstant != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toInstant));
            }
            if (StringUtils.hasText(search)) {
                String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("action")), like),
                        cb.like(cb.lower(root.get("actorEmail")), like),
                        cb.like(cb.lower(root.get("entityType")), like),
                        cb.like(cb.lower(root.get("summary")), like)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Pageable toSortedPageable(int page, int size, String sortBy, String sortDir) {
        String property = SORTABLE.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction =
                "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        int safeSize = Math.min(Math.max(size, 1), 200);
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, safeSize, Sort.by(direction, property));
    }

    private Instant parseInstant(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (Exception ex) {
            try {
                return Instant.parse(raw + "T00:00:00Z");
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private IngobokaUserDetails safeCurrentUser() {
        try {
            return SecurityUtils.currentUser();
        } catch (Exception ex) {
            return null;
        }
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .organizationId(log.getOrganizationId())
                .actorUserId(log.getActorUserId())
                .actorEmail(log.getActorEmail())
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .correlationId(log.getCorrelationId())
                .summary(log.getSummary())
                .outcome(log.getOutcome() != null ? log.getOutcome() : "SUCCESS")
                .createdAt(log.getCreatedAt())
                .build();
    }

    private DataSubjectRequestResponse toDataSubjectResponse(DataSubjectRequest request) {
        return DataSubjectRequestResponse.builder()
                .id(request.getId())
                .userId(request.getUserId())
                .requestType(request.getRequestType())
                .status(request.getStatus())
                .details(request.getDetails())
                .resolvedAt(request.getResolvedAt())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
