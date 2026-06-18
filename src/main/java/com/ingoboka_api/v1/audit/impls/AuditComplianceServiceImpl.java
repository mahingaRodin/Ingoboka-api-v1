package com.ingoboka_api.v1.audit.impls;

import com.ingoboka_api.v1.audit.models.AuditLog;
import com.ingoboka_api.v1.audit.models.DataSubjectRequest;
import com.ingoboka_api.v1.audit.repositories.AuditLogRepository;
import com.ingoboka_api.v1.audit.repositories.DataSubjectRequestRepository;
import com.ingoboka_api.v1.audit.services.AuditComplianceService;
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
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditComplianceServiceImpl implements AuditComplianceService {

    private final AuditLogRepository auditLogRepository;
    private final DataSubjectRequestRepository dataSubjectRequestRepository;

    @Override
    @Transactional
    public void log(String action, String entityType, UUID entityId, String summary) {
        IngobokaUserDetails actor = safeCurrentUser();
        AuditLog entry = new AuditLog();
        entry.setId(UUID.randomUUID());
        entry.setOrganizationId(actor != null ? actor.getOrganizationId() : null);
        entry.setActorUserId(actor != null ? actor.getUserId() : null);
        entry.setActorEmail(actor != null ? actor.getEmail() : "system");
        entry.setAction(action);
        entry.setEntityType(entityType);
        entry.setEntityId(entityId);
        entry.setCorrelationId(MDC.get("correlationId"));
        entry.setSummary(summary);
        entry.setCreatedAt(Instant.now());
        auditLogRepository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> listAuditLogs(int page, int size) {
        if (!SecurityUtils.currentUser().hasRole(RoleCodes.PLATFORM_ADMIN)) {
            throw new BusinessException("Only platform administrators can view all audit logs");
        }
        Page<AuditLog> result =
                auditLogRepository.findAllByOrderByCreatedAtDesc(PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> listTenantAuditLogs(int page, int size) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (!user.hasRole(RoleCodes.COMPLIANCE_AUDITOR)
                && !user.hasRole(RoleCodes.PARTNER_ADMIN)
                && !user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            throw new BusinessException("Access denied");
        }
        if (user.getOrganizationId() == null) {
            throw new BusinessException("No organization associated with this account");
        }
        Page<AuditLog> result = auditLogRepository.findByOrganizationIdOrderByCreatedAtDesc(
                user.getOrganizationId(), PaginationUtils.toPageable(page, size));
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
        log("DATA_SUBJECT_REQUEST_RESOLVED", "DATA_SUBJECT_REQUEST", entry.getId(), request.getStatus().name());
        return toDataSubjectResponse(entry);
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
