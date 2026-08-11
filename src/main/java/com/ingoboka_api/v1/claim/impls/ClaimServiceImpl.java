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
import com.ingoboka_api.v1.common.enums.AuditOutcome;
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
import com.ingoboka_api.v1.common.requests.UpdateClaimRequest;
import com.ingoboka_api.v1.common.requests.UpdateClaimStatusRequest;
import com.ingoboka_api.v1.common.responses.ClaimAppealResponse;
import com.ingoboka_api.v1.common.responses.ClaimsBreakdownResponse;
import com.ingoboka_api.v1.common.responses.ClaimResponse;
import com.ingoboka_api.v1.common.responses.ClaimStatusHistoryItemResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.common.util.HashUtils;
import com.ingoboka_api.v1.common.util.PaginationUtils;
import com.ingoboka_api.v1.customer.models.CitizenProfile;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.document.services.DocumentStorageService;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.messaging.services.InsurerStaffNotificationService;
import com.ingoboka_api.v1.messaging.services.NotificationTemplateService;
import com.ingoboka_api.v1.policy.models.Policy;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ClaimServiceImpl implements ClaimService {

    private static final Set<ClaimStatus> APPEALABLE = Set.of(ClaimStatus.REJECTED, ClaimStatus.APPROVED);

    private static final Set<ClaimStatus> CANCELLABLE = Set.of(ClaimStatus.DRAFT, ClaimStatus.SUBMITTED);

    private static final Map<String, List<String>> PROVINCE_DISTRICTS = buildProvinceDistricts();

    private static Map<String, List<String>> buildProvinceDistricts() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("City of Kigali", List.of("Gasabo", "Kicukiro", "Nyarugenge"));
        map.put("Eastern Province", List.of("Bugesera", "Gatsibo", "Kayonza", "Kirehe", "Ngoma", "Nyagatare", "Rwamagana"));
        map.put("Northern Province", List.of("Burera", "Gakenke", "Gicumbi", "Musanze", "Rulindo"));
        map.put("Southern Province", List.of("Gisagara", "Huye", "Kamonyi", "Muhanga", "Nyamagabe", "Nyanza", "Nyaruguru", "Ruhango"));
        map.put("Western Province", List.of("Karongi", "Ngororero", "Nyabihu", "Nyamasheke", "Rubavu", "Rusizi", "Rutsiro"));
        return map;
    }

    private final ClaimRepository claimRepository;
    private final ClaimDocumentRepository claimDocumentRepository;
    private final ClaimStatusHistoryRepository claimStatusHistoryRepository;
    private final ClaimDecisionRepository claimDecisionRepository;
    private final ClaimAppealRepository claimAppealRepository;
    private final PolicyRepository policyRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final NotificationTemplateService notificationTemplateService;
    private final InsurerStaffNotificationService insurerStaffNotificationService;
    private final AuditComplianceService auditComplianceService;
    private final UserRepository userRepository;
    private final DocumentStorageService documentStorageService;

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
        claim.setIncidentDate(request.getIncidentDate());
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
        if (claimDocumentRepository.countByClaimId(claimId) < 1) {
            throw new BusinessException("At least one supporting document is required before submitting a claim");
        }
        transitionStatus(claim, ClaimStatus.SUBMITTED, "Claim submitted by policyholder", null);
        claim.setUpdatedAt(Instant.now());
        claimRepository.save(claim);
        notifyClaimholder(claim, "CLAIM_SUBMITTED", Map.of(
                "claimNumber", claim.getClaimNumber(),
                "decision", "SUBMITTED",
                "notes", "Your claim is now under review."));
        notifyInsurerStaffSubmitted(claim);
        auditComplianceService.log("CLAIM_SUBMITTED", "CLAIM", claim.getId(), "Claim submitted");
        return toResponse(claim);
    }

    @Override
    @Transactional
    public ClaimResponse cancelClaim(UUID claimId) {
        Claim claim = requireOwnedClaim(claimId);
        if (!CANCELLABLE.contains(claim.getStatus())) {
            throw new BusinessException("Only pending claims can be cancelled");
        }
        UUID actorId = SecurityUtils.currentUser().getUserId();
        transitionStatus(claim, ClaimStatus.CANCELLED, "Claim withdrawn by policyholder", actorId);
        claim.setUpdatedAt(Instant.now());
        claimRepository.save(claim);
        auditComplianceService.log("CLAIM_CANCELLED", "CLAIM", claim.getId(), "Claim cancelled by policyholder");
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
        return listTenantClaimsFiltered(status, null, null, null, "createdAt", "desc", page, size);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> listTenantClaimsFiltered(
            ClaimStatus status,
            String search,
            String province,
            String district,
            String sortBy,
            String sortDir,
            int page,
            int size) {
        UUID orgId = requireClaimsOrganizationId();
        List<String> districtsInProvince = Collections.emptyList();
        if (province != null && !province.isBlank()) {
            districtsInProvince = PROVINCE_DISTRICTS.getOrDefault(province, List.of("__none__"));
        }
        String sortProperty = sortBy != null && !sortBy.isBlank() ? sortBy : "createdAt";
        Page<Claim> result = claimRepository.findTenantClaimsFiltered(
                orgId,
                status,
                search,
                district,
                province,
                districtsInProvince,
                PaginationUtils.toPageable(page, size, sortProperty, "desc".equalsIgnoreCase(sortDir) ? "desc" : "asc"));
        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    @Transactional
    public ClaimResponse createTenantClaim(CreateClaimRequest request) {
        UUID orgId = requireClaimsOrganizationId();
        Policy policy = policyRepository
                .findById(request.getPolicyId())
                .orElseThrow(() -> new BusinessException("Policy not found"));
        if (!policy.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Policy does not belong to this insurer");
        }
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new BusinessException("Claims can only be filed against active policies");
        }

        Instant now = Instant.now();
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setClaimNumber(generateClaimNumber());
        claim.setPolicyId(policy.getId());
        claim.setOrganizationId(policy.getOrganizationId());
        claim.setCitizenProfileId(policy.getCitizenProfileId());
        claim.setClaimType(request.getClaimType());
        claim.setDescription(request.getDescription());
        claim.setClaimedAmount(request.getClaimedAmount());
        claim.setIncidentDate(request.getIncidentDate());
        claim.setStatus(ClaimStatus.SUBMITTED);
        claim.setCreatedAt(now);
        claim.setUpdatedAt(now);
        claimRepository.save(claim);
        UUID actorId = SecurityUtils.currentUser().getUserId();
        transitionStatus(claim, ClaimStatus.SUBMITTED, "Created by insurer staff", actorId);
        claimRepository.save(claim);
        notifyInsurerStaffSubmitted(claim);
        return toResponse(claim);
    }

    @Override
    @Transactional
    public ClaimResponse updateClaim(UUID claimId, UpdateClaimRequest request) {
        requireClaimsOrganizationId();
        Claim claim = claimRepository
                .findById(claimId)
                .orElseThrow(() -> new BusinessException("Claim not found"));
        assertTenantAccess(claim);
        if (claim.getStatus() == ClaimStatus.APPROVED || claim.getStatus() == ClaimStatus.REJECTED) {
            throw new BusinessException("Cannot update a finalized claim");
        }
        if (request.getClaimType() != null) claim.setClaimType(request.getClaimType());
        if (request.getDescription() != null) claim.setDescription(request.getDescription());
        if (request.getClaimedAmount() != null) claim.setClaimedAmount(request.getClaimedAmount());
        if (request.getIncidentDate() != null) claim.setIncidentDate(request.getIncidentDate());
        claim.setUpdatedAt(Instant.now());
        claimRepository.save(claim);
        notifyClaimholder(claim, "CLAIM_UPDATED", Map.of(
                "claimNumber", claim.getClaimNumber(),
                "notes", "Review your claim for the latest details."));
        return toResponse(claim);
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
        requireDecisionReason(request.getStatus(), request.getReason());

        UUID actorId = SecurityUtils.currentUser().getUserId();
        transitionStatus(claim, request.getStatus(), request.getReason(), actorId);
        claim.setUpdatedAt(Instant.now());
        claimRepository.save(claim);
        notifyClaimholder(claim, "CLAIM_STATUS_CHANGE", Map.of(
                "claimNumber", claim.getClaimNumber(),
                "status", request.getStatus().name(),
                "notes", request.getReason() != null ? request.getReason() : ""));
        notifyInsurerStaffStatusChange(claim, request.getStatus(), request.getReason());
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
        requireDecisionReason(
                switch (request.getDecision()) {
                    case APPROVED, PARTIAL -> ClaimStatus.APPROVED;
                    case REJECTED -> ClaimStatus.REJECTED;
                },
                request.getReason());

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
        notifyInsurerStaffDecision(claim, decisionLabel, request.getReason());
        if (request.getDecision() == ClaimDecisionType.APPROVED
                || request.getDecision() == ClaimDecisionType.PARTIAL) {
            notifyClaimholder(claim, "PAYOUT_READY", Map.of(
                    "policyNumber",
                    policyRepository
                            .findById(claim.getPolicyId())
                            .map(p -> p.getPolicyNumber())
                            .orElse(""),
                    "amount",
                    request.getApprovedAmount() != null
                            ? request.getApprovedAmount().toPlainString()
                            : claim.getClaimedAmount().toPlainString(),
                    "currency", "RWF"));
        }
        AuditOutcome outcome = request.getDecision() == ClaimDecisionType.REJECTED
                ? AuditOutcome.FAILED
                : AuditOutcome.SUCCESS;
        auditComplianceService.log(
                outcome,
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
    public void uploadDocuments(UUID claimId, MultipartFile[] files) {
        if (files == null || files.length == 0) {
            throw new BusinessException("At least one file is required");
        }
        int uploaded = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String filename = file.getOriginalFilename();
            if (filename == null || filename.isBlank()) {
                filename = "document";
            }
            filename = filename.replace("\\", "_").replace("/", "_");
            String objectKey = "claims/" + claimId + "/" + UUID.randomUUID() + "-" + filename;
            String contentType =
                    file.getContentType() != null && !file.getContentType().isBlank()
                            ? file.getContentType()
                            : "application/octet-stream";
            long size = file.getSize();
            try {
                byte[] bytes = file.getBytes();
                String checksum = HashUtils.sha256(bytes);
                documentStorageService.upload(
                        objectKey, new ByteArrayInputStream(bytes), size, contentType);

                AttachClaimDocumentRequest attachReq = new AttachClaimDocumentRequest();
                attachReq.setDocumentType("CLAIM_EVIDENCE");
                attachReq.setObjectKey(objectKey);
                attachReq.setMimeType(contentType);
                attachReq.setSizeBytes(size);
                attachReq.setChecksum(checksum != null ? checksum : "unknown");
                attachDocument(claimId, attachReq);
                uploaded++;
            } catch (IOException ex) {
                throw new BusinessException("Failed to read uploaded file: " + ex.getMessage());
            }
        }
        if (uploaded == 0) {
            throw new BusinessException("At least one non-empty file is required");
        }
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

    private void requireDecisionReason(ClaimStatus status, String reason) {
        if (!requiresDecisionReason(status)) {
            return;
        }
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("A reason is required for this claim decision");
        }
    }

    private boolean requiresDecisionReason(ClaimStatus status) {
        return status == ClaimStatus.APPROVED
                || status == ClaimStatus.REJECTED
                || status == ClaimStatus.INFORMATION_REQUIRED;
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
                .ifPresent(user -> notificationTemplateService.notifyAllChannels(
                        user.getId(),
                        claim.getOrganizationId(),
                        templateCode,
                        user.getEmail(),
                        user.getPhoneNumber(),
                        variables,
                        1,
                        "CLAIM",
                        claim.getId())));
    }

    private void notifyInsurerStaffSubmitted(Claim claim) {
        ClaimContext ctx = resolveClaimContext(claim);
        insurerStaffNotificationService.notifyClaimSubmitted(claim, ctx.claimantName(), ctx.policyNumber());
    }

    private void notifyInsurerStaffStatusChange(Claim claim, ClaimStatus status, String reason) {
        ClaimContext ctx = resolveClaimContext(claim);
        insurerStaffNotificationService.notifyClaimStatusChange(
                claim, status, ctx.claimantName(), ctx.policyNumber(), reason);
    }

    private void notifyInsurerStaffDecision(Claim claim, String decisionLabel, String reason) {
        ClaimContext ctx = resolveClaimContext(claim);
        insurerStaffNotificationService.notifyClaimDecision(
                claim, decisionLabel, ctx.claimantName(), ctx.policyNumber(), reason);
    }

    private ClaimContext resolveClaimContext(Claim claim) {
        String policyNumber = policyRepository
                .findById(claim.getPolicyId())
                .map(Policy::getPolicyNumber)
                .orElse("");
        String claimantName = citizenProfileRepository
                .findById(claim.getCitizenProfileId())
                .flatMap(profile -> userRepository.findById(profile.getUserId()))
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse("Policyholder");
        return new ClaimContext(claimantName, policyNumber);
    }

    private record ClaimContext(String claimantName, String policyNumber) {}

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
                .incidentDate(claim.getIncidentDate())
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
