package com.ingoboka_api.v1.claim.impls;

import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import com.ingoboka_api.v1.claim.models.Claim;
import com.ingoboka_api.v1.claim.models.ClaimAppeal;
import com.ingoboka_api.v1.claim.models.ClaimDecision;
import com.ingoboka_api.v1.claim.models.ClaimDocument;
import com.ingoboka_api.v1.claim.models.ClaimStatusHistory;
import com.ingoboka_api.v1.claim.repositories.ClaimAppealRepository;
import com.ingoboka_api.v1.claim.repositories.ClaimDecisionRepository;
import com.ingoboka_api.v1.claim.repositories.ClaimDocumentRepository;
import com.ingoboka_api.v1.claim.repositories.ClaimRepository;
import com.ingoboka_api.v1.claim.repositories.ClaimStatusHistoryRepository;
import com.ingoboka_api.v1.claim.services.ClaimService;
import com.ingoboka_api.v1.common.enums.AppealStatus;
import com.ingoboka_api.v1.common.enums.ClaimDecisionType;
import com.ingoboka_api.v1.common.enums.ClaimStatus;
import com.ingoboka_api.v1.common.enums.DocumentAccessClassification;
import com.ingoboka_api.v1.common.enums.NotificationChannel;
import com.ingoboka_api.v1.common.enums.PolicyStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.AttachClaimDocumentRequest;
import com.ingoboka_api.v1.common.requests.CreateClaimAppealRequest;
import com.ingoboka_api.v1.common.requests.CreateClaimRequest;
import com.ingoboka_api.v1.common.requests.RecordClaimDecisionRequest;
import com.ingoboka_api.v1.common.requests.UpdateClaimStatusRequest;
import com.ingoboka_api.v1.common.responses.ClaimAppealResponse;
import com.ingoboka_api.v1.common.responses.ClaimsBreakdownResponse;
import com.ingoboka_api.v1.common.responses.ClaimResponse;
import com.ingoboka_api.v1.common.responses.ClaimStatusHistoryItemResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.common.util.PaginationUtils;
import com.ingoboka_api.v1.customer.models.CitizenProfile;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.messaging.services.NotificationTemplateService;
import com.ingoboka_api.v1.policy.models.Policy;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private static final Set<ClaimStatus> APPEALABLE = Set.of(ClaimStatus.REJECTED, ClaimStatus.APPROVED);

    private final ClaimRepository claimRepository;
    private final ClaimDocumentRepository claimDocumentRepository;
    private final ClaimStatusHistoryRepository claimStatusHistoryRepository;
    private final ClaimDecisionRepository claimDecisionRepository;
    private final ClaimAppealRepository claimAppealRepository;
    private final PolicyRepository policyRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final NotificationTemplateService notificationTemplateService;
    private final AuditComplianceService auditComplianceService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ClaimResponse createClaim(CreateClaimRequest request) {
        CitizenProfile profile = requireMyProfile();
        Policy policy = requireActivePolicy(request.getPolicyId(), profile.getId());

        Instant now = Instant.now();
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setClaimNumber(generateClaimNumber());
        claim.setPolicyId(policy.getId());
        claim.setOrganizationId(policy.getOrganizationId());
        claim.setCitizenProfileId(profile.getId());
        claim.setClaimType(request.getClaimType());
        claim.setDescription(request.getDescription());
        claim.setClaimedAmount(request.getClaimedAmount());
        claim.setStatus(ClaimStatus.DRAFT);
        claim.setCreatedAt(now);
        claim.setUpdatedAt(now);
        claimRepository.save(claim);
        return toResponse(claim);
    }

    @Override
    @Transactional
    public ClaimResponse submitClaim(UUID claimId) {
        Claim claim = requireOwnedClaim(claimId);
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new BusinessException("Only draft claims can be submitted");
        }
        transitionStatus(claim, ClaimStatus.SUBMITTED, "Claim submitted by policyholder", null);
        claim.setUpdatedAt(Instant.now());
        claimRepository.save(claim);
        notifyClaimholder(claim, "CLAIM_SUBMITTED", Map.of(
                "claimNumber", claim.getClaimNumber(),
                "decision", "SUBMITTED",
                "notes", "Your claim is now under review."));
        auditComplianceService.log("CLAIM_SUBMITTED", "CLAIM", claim.getId(), "Claim submitted");
        return toResponse(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimResponse getClaim(UUID claimId) {
        Claim claim = claimRepository
                .findById(claimId)
                .orElseThrow(() -> new BusinessException("Claim not found"));
        assertCanAccessClaim(claim);
        return toResponse(claim);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> listMyClaims(int page, int size) {
        CitizenProfile profile = requireMyProfile();
        Page<Claim> result = claimRepository.findByCitizenProfileIdOrderByCreatedAtDesc(
                profile.getId(), PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> listTenantClaims(ClaimStatus status, int page, int size) {
        UUID orgId = requireClaimsOrganizationId();
        Page<Claim> result = status == null
                ? claimRepository.findByOrganizationIdOrderByCreatedAtDesc(
                        orgId, PaginationUtils.toPageable(page, size))
                : claimRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(
                        orgId, status, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    @Transactional
    public ClaimResponse updateStatus(UUID claimId, UpdateClaimStatusRequest request) {
        requireClaimsOrganizationId();
        Claim claim = claimRepository
                .findById(claimId)
                .orElseThrow(() -> new BusinessException("Claim not found"));
        assertTenantAccess(claim);

        if (request.getStatus() == ClaimStatus.DRAFT) {
            throw new BusinessException("Cannot move claim back to draft");
        }

        UUID actorId = SecurityUtils.currentUser().getUserId();
        transitionStatus(claim, request.getStatus(), request.getReason(), actorId);
        claim.setUpdatedAt(Instant.now());
        claimRepository.save(claim);
        return toResponse(claim);
    }

    @Override
    @Transactional
    public ClaimResponse recordDecision(UUID claimId, RecordClaimDecisionRequest request) {
        requireClaimsOrganizationId();
        Claim claim = claimRepository
                .findById(claimId)
                .orElseThrow(() -> new BusinessException("Claim not found"));
        assertTenantAccess(claim);

        if (claimDecisionRepository.findByClaimId(claimId).isPresent()) {
            throw new BusinessException("Decision already recorded for this claim");
        }

        ClaimStatus targetStatus = switch (request.getDecision()) {
            case APPROVED -> ClaimStatus.APPROVED;
            case REJECTED -> ClaimStatus.REJECTED;
            case PARTIAL -> ClaimStatus.APPROVED;
        };

        UUID actorId = SecurityUtils.currentUser().getUserId();
        ClaimDecision decision = new ClaimDecision();
        decision.setId(UUID.randomUUID());
        decision.setClaimId(claimId);
        decision.setDecision(request.getDecision());
        decision.setApprovedAmount(request.getApprovedAmount());
        decision.setReason(request.getReason());
        decision.setDecidedBy(actorId);
        decision.setDecidedAt(Instant.now());
        claimDecisionRepository.save(decision);

        transitionStatus(claim, targetStatus, request.getReason(), actorId);
        claim.setUpdatedAt(Instant.now());
        claimRepository.save(claim);

        String decisionLabel = formatDecision(request.getDecision(), request.getApprovedAmount());
        notifyClaimholder(claim, "CLAIM_DECISION", Map.of(
                "claimNumber", claim.getClaimNumber(),
                "decision", decisionLabel,
                "notes", request.getReason() != null ? request.getReason() : ""));
        auditComplianceService.log(
                "CLAIM_DECISION_RECORDED",
                "CLAIM",
                claim.getId(),
                request.getDecision() + ": " + decisionLabel);

        return toResponse(claim);
    }

    @Override
    @Transactional
    public void attachDocument(UUID claimId, AttachClaimDocumentRequest request) {
        Claim claim = claimRepository
                .findById(claimId)
                .orElseThrow(() -> new BusinessException("Claim not found"));
        assertCanAccessClaim(claim);

        ClaimDocument document = new ClaimDocument();
        document.setId(UUID.randomUUID());
        document.setClaimId(claimId);
        document.setDocumentType(request.getDocumentType());
        document.setObjectKey(request.getObjectKey());
        document.setMimeType(request.getMimeType());
        document.setSizeBytes(request.getSizeBytes());
        document.setChecksum(request.getChecksum());
        document.setAccessClassification(
                request.getAccessClassification() != null
                        ? request.getAccessClassification()
                        : DocumentAccessClassification.INTERNAL);
        document.setCreatedAt(Instant.now());
        claimDocumentRepository.save(document);
    }

    @Override
    @Transactional
    public ClaimAppealResponse createAppeal(UUID claimId, CreateClaimAppealRequest request) {
        Claim claim = requireOwnedClaim(claimId);
        if (!APPEALABLE.contains(claim.getStatus())) {
            throw new BusinessException("This claim is not eligible for appeal");
        }

        Instant now = Instant.now();
        ClaimAppeal appeal = new ClaimAppeal();
        appeal.setId(UUID.randomUUID());
        appeal.setClaimId(claimId);
        appeal.setReason(request.getReason());
        appeal.setStatus(AppealStatus.SUBMITTED);
        appeal.setSubmittedAt(now);
        claimAppealRepository.save(appeal);
        return toAppealResponse(appeal);
    }

    private Policy requireActivePolicy(UUID policyId, UUID citizenProfileId) {
        Policy policy = policyRepository
                .findById(policyId)
                .orElseThrow(() -> new BusinessException("Policy not found"));
        if (!policy.getCitizenProfileId().equals(citizenProfileId)) {
            throw new BusinessException("Policy does not belong to this citizen");
        }
        if (policy.getStatus() != PolicyStatus.ACTIVE && policy.getStatus() != PolicyStatus.GRACE_PERIOD) {
            throw new BusinessException("Claims can only be filed against active policies");
        }
        return policy;
    }

    private Claim requireOwnedClaim(UUID claimId) {
        CitizenProfile profile = requireMyProfile();
        Claim claim = claimRepository
                .findById(claimId)
                .orElseThrow(() -> new BusinessException("Claim not found"));
        if (!claim.getCitizenProfileId().equals(profile.getId())) {
            throw new BusinessException("Access denied");
        }
        return claim;
    }

    private void transitionStatus(Claim claim, ClaimStatus toStatus, String reason, UUID changedBy) {
        ClaimStatus fromStatus = claim.getStatus();
        claim.setStatus(toStatus);

        ClaimStatusHistory history = new ClaimStatusHistory();
        history.setId(UUID.randomUUID());
        history.setClaimId(claim.getId());
        history.setFromStatus(fromStatus);
        history.setToStatus(toStatus);
        history.setReason(reason);
        history.setChangedBy(changedBy);
        history.setCreatedAt(Instant.now());
        claimStatusHistoryRepository.save(history);
    }

    private void assertCanAccessClaim(Claim claim) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            return;
        }
        if (user.hasRole(RoleCodes.CLAIMS_OFFICER)
                || user.hasRole(RoleCodes.CLAIMS_SUPERVISOR)
                || user.hasRole(RoleCodes.PARTNER_ADMIN)) {
            if (claim.getOrganizationId().equals(user.getOrganizationId())) {
                return;
            }
        }
        CitizenProfile profile = citizenProfileRepository
                .findByUserId(user.getUserId())
                .orElseThrow(() -> new BusinessException("Access denied"));
        if (claim.getCitizenProfileId().equals(profile.getId())) {
            return;
        }
        throw new BusinessException("Access denied to this claim");
    }

    private void assertTenantAccess(Claim claim) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            return;
        }
        if (!claim.getOrganizationId().equals(user.getOrganizationId())) {
            throw new BusinessException("Claim is outside your tenant");
        }
    }

    private UUID requireClaimsOrganizationId() {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (user.getOrganizationId() == null) {
            throw new BusinessException("No organization associated with this account");
        }
        if (!user.hasRole(RoleCodes.CLAIMS_OFFICER)
                && !user.hasRole(RoleCodes.CLAIMS_SUPERVISOR)
                && !user.hasRole(RoleCodes.PARTNER_ADMIN)
                && !user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            throw new BusinessException("Only claims officers can perform this action");
        }
        return user.getOrganizationId();
    }

    private CitizenProfile requireMyProfile() {
        return citizenProfileRepository
                .findByUserId(SecurityUtils.currentUser().getUserId())
                .orElseThrow(() -> new BusinessException("Citizen profile not found"));
    }

    private String generateClaimNumber() {
        return "CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String formatDecision(ClaimDecisionType decision, java.math.BigDecimal approvedAmount) {
        return switch (decision) {
            case APPROVED -> "APPROVED"
                    + (approvedAmount != null ? " (RWF " + approvedAmount + ")" : "");
            case REJECTED -> "REJECTED";
            case PARTIAL -> "PARTIALLY APPROVED"
                    + (approvedAmount != null ? " (RWF " + approvedAmount + ")" : "");
        };
    }

    private void notifyClaimholder(Claim claim, String templateCode, Map<String, String> variables) {
        citizenProfileRepository.findById(claim.getCitizenProfileId()).ifPresent(profile -> userRepository
                .findById(profile.getUserId())
                .ifPresent(user -> notificationTemplateService.sendTemplated(
                        user.getId(),
                        claim.getOrganizationId(),
                        templateCode,
                        NotificationChannel.EMAIL,
                        user.getEmail(),
                        variables)));
    }

    private ClaimResponse toResponse(Claim claim) {
        Policy policy = policyRepository.findById(claim.getPolicyId()).orElse(null);
        String policyNumber = policy != null ? policy.getPolicyNumber() : null;
        String claimantName = citizenProfileRepository
                .findById(claim.getCitizenProfileId())
                .flatMap(profile -> userRepository.findById(profile.getUserId()))
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse("Citizen");
        List<ClaimStatusHistoryItemResponse> history = claimStatusHistoryRepository
                .findByClaimIdOrderByCreatedAtAsc(claim.getId())
                .stream()
                .map(item -> ClaimStatusHistoryItemResponse.builder()
                        .status(item.getToStatus())
                        .label(item.getToStatus().name())
                        .occurredAt(item.getCreatedAt())
                        .note(item.getReason())
                        .build())
                .toList();
        return ClaimResponse.builder()
                .id(claim.getId())
                .claimNumber(claim.getClaimNumber())
                .policyId(claim.getPolicyId())
                .organizationId(claim.getOrganizationId())
                .claimType(claim.getClaimType())
                .description(claim.getDescription())
                .claimedAmount(claim.getClaimedAmount())
                .currency("RWF")
                .policyNumber(policyNumber)
                .claimantName(claimantName)
                .status(claim.getStatus())
                .statusHistory(history)
                .createdAt(claim.getCreatedAt())
                .updatedAt(claim.getUpdatedAt())
                .build();
    }

    private ClaimAppealResponse toAppealResponse(ClaimAppeal appeal) {
        return ClaimAppealResponse.builder()
                .id(appeal.getId())
                .claimId(appeal.getClaimId())
                .reason(appeal.getReason())
                .status(appeal.getStatus())
                .submittedAt(appeal.getSubmittedAt())
                .reviewedAt(appeal.getReviewedAt())
                .reviewNotes(appeal.getReviewNotes())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ClaimsBreakdownResponse getClaimsBreakdown() {
        UUID orgId = SecurityUtils.currentUser().getOrganizationId();
        if (orgId == null && !SecurityUtils.currentUser().hasRole(RoleCodes.PLATFORM_ADMIN)) {
            throw new BusinessException("No organization associated with this account");
        }
        List<Claim> claims = orgId != null
                ? claimRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId)
                : claimRepository.findAll();
        Instant startOfDay = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        long resolvedToday = claims.stream()
                .filter(claim -> claim.getStatus() == ClaimStatus.APPROVED || claim.getStatus() == ClaimStatus.REJECTED)
                .filter(claim -> claim.getUpdatedAt() != null && claim.getUpdatedAt().isAfter(startOfDay))
                .count();
        double avgResolutionDays = claims.stream()
                .filter(claim -> claim.getStatus() == ClaimStatus.APPROVED || claim.getStatus() == ClaimStatus.REJECTED)
                .filter(claim -> claim.getCreatedAt() != null && claim.getUpdatedAt() != null)
                .mapToLong(claim -> ChronoUnit.DAYS.between(claim.getCreatedAt(), claim.getUpdatedAt()))
                .average()
                .orElse(0.0);
        List<ClaimsBreakdownResponse.StatusCount> byStatus = Arrays.stream(ClaimStatus.values())
                .map(status -> ClaimsBreakdownResponse.StatusCount.builder()
                        .status(status)
                        .count(claims.stream().filter(claim -> claim.getStatus() == status).count())
                        .build())
                .filter(item -> item.getCount() > 0)
                .toList();
        return ClaimsBreakdownResponse.builder()
                .resolvedToday(resolvedToday)
                .avgResolutionDays(avgResolutionDays)
                .claimsByStatus(byStatus)
                .build();
    }
}
