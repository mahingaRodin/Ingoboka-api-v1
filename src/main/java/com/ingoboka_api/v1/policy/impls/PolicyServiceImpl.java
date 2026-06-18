package com.ingoboka_api.v1.policy.impls;

import com.ingoboka_api.v1.common.enums.DocumentAccessClassification;
import com.ingoboka_api.v1.common.enums.PolicyMemberType;
import com.ingoboka_api.v1.common.enums.PolicyStatus;
import com.ingoboka_api.v1.common.enums.PremiumScheduleStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.AttachPolicyDocumentRequest;
import com.ingoboka_api.v1.common.responses.PolicyResponse;
import com.ingoboka_api.v1.common.responses.PolicyVerificationResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.customer.models.CitizenProfile;
import com.ingoboka_api.v1.customer.models.Dependant;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.customer.repositories.DependantRepository;
import com.ingoboka_api.v1.enrollment.models.PolicyApplication;
import com.ingoboka_api.v1.identity.models.Organization;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.identity.services.OrganizationManagementService;
import com.ingoboka_api.v1.billing.models.PremiumSchedule;
import com.ingoboka_api.v1.billing.repositories.PremiumScheduleRepository;
import com.ingoboka_api.v1.policy.models.Policy;
import com.ingoboka_api.v1.policy.models.PolicyDocument;
import com.ingoboka_api.v1.policy.models.PolicyMember;
import com.ingoboka_api.v1.policy.repositories.PolicyDocumentRepository;
import com.ingoboka_api.v1.policy.repositories.PolicyMemberRepository;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import com.ingoboka_api.v1.policy.services.PolicyIssuanceService;
import com.ingoboka_api.v1.policy.services.PolicyService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicyServiceImpl implements PolicyService, PolicyIssuanceService {

    private final PolicyRepository policyRepository;
    private final PolicyMemberRepository policyMemberRepository;
    private final PolicyDocumentRepository policyDocumentRepository;
    private final PremiumScheduleRepository premiumScheduleRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final DependantRepository dependantRepository;
    private final UserRepository userRepository;
    private final OrganizationManagementService organizationManagementService;

    @Override
    @Transactional
    public PolicyResponse issueFromApprovedApplication(PolicyApplication application) {
        if (policyRepository.existsByApplicationId(application.getId())) {
            return toResponse(policyRepository.findByApplicationId(application.getId()).orElseThrow());
        }

        Instant now = Instant.now();
        Policy policy = new Policy();
        policy.setId(UUID.randomUUID());
        policy.setPolicyNumber(generatePolicyNumber());
        policy.setOrganizationId(application.getOrganizationId());
        policy.setApplicationId(application.getId());
        policy.setCitizenProfileId(application.getCitizenProfileId());
        policy.setProductPlanId(application.getProductPlanId());
        policy.setStatus(PolicyStatus.PENDING_PAYMENT);
        policy.setPremiumAmount(application.getPremiumAmount());
        policy.setPremiumFrequency(application.getPremiumFrequency());
        policy.setQrVerificationToken(UUID.randomUUID().toString().replace("-", ""));
        policy.setCreatedAt(now);
        policy.setUpdatedAt(now);
        policyRepository.save(policy);

        addPolicyMembers(policy, now);
        createInitialPremiumSchedule(policy, now);
        return toResponse(policy);
    }

    @Override
    @Transactional
    public void activatePolicy(UUID policyId) {
        Policy policy = policyRepository
                .findById(policyId)
                .orElseThrow(() -> new BusinessException("Policy not found"));
        if (policy.getStatus() != PolicyStatus.PENDING_PAYMENT) {
            throw new BusinessException("Policy is not awaiting payment");
        }
        LocalDate startDate = LocalDate.now();
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setStartDate(startDate);
        policy.setEndDate(startDate.plusYears(1));
        policy.setUpdatedAt(Instant.now());
        policyRepository.save(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyResponse getPolicy(UUID policyId) {
        Policy policy = policyRepository
                .findById(policyId)
                .orElseThrow(() -> new BusinessException("Policy not found"));
        assertCanAccessPolicy(policy);
        return toResponse(policy);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyResponse> listMyPolicies() {
        CitizenProfile profile = requireMyProfile();
        return policyRepository.findByCitizenProfileIdOrderByCreatedAtDesc(profile.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PolicyResponse> listTenantPolicies() {
        UUID orgId = requireTenantOrganizationId();
        return policyRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyVerificationResponse verifyByQrToken(String token) {
        Policy policy = policyRepository
                .findByQrVerificationToken(token)
                .orElseThrow(() -> new BusinessException("Invalid verification token"));

        String insurerCode = organizationManagementService
                .findById(policy.getOrganizationId())
                .map(Organization::getCode)
                .orElse("UNKNOWN");

        boolean valid = policy.getStatus() == PolicyStatus.ACTIVE
                || policy.getStatus() == PolicyStatus.GRACE_PERIOD;

        return PolicyVerificationResponse.builder()
                .policyNumber(policy.getPolicyNumber())
                .status(policy.getStatus())
                .startDate(policy.getStartDate())
                .endDate(policy.getEndDate())
                .insurerCode(insurerCode)
                .valid(valid)
                .build();
    }

    @Override
    @Transactional
    public void attachDocument(UUID policyId, AttachPolicyDocumentRequest request) {
        Policy policy = policyRepository
                .findById(policyId)
                .orElseThrow(() -> new BusinessException("Policy not found"));
        assertCanAccessPolicy(policy);

        PolicyDocument document = new PolicyDocument();
        document.setId(UUID.randomUUID());
        document.setPolicyId(policyId);
        document.setDocumentType(request.getDocumentType());
        document.setObjectKey(request.getObjectKey());
        document.setMimeType(request.getMimeType());
        document.setSizeBytes(request.getSizeBytes());
        document.setChecksum(request.getChecksum());
        document.setAccessClassification(
                request.getAccessClassification() != null
                        ? request.getAccessClassification()
                        : DocumentAccessClassification.CUSTOMER);
        document.setCreatedAt(Instant.now());
        policyDocumentRepository.save(document);
    }

    @Override
    @Transactional(readOnly = true)
    public Policy requirePolicyForPayment(UUID policyId, UUID citizenProfileId) {
        Policy policy = policyRepository
                .findById(policyId)
                .orElseThrow(() -> new BusinessException("Policy not found"));
        if (!policy.getCitizenProfileId().equals(citizenProfileId)) {
            throw new BusinessException("Policy does not belong to this citizen");
        }
        if (policy.getStatus() != PolicyStatus.PENDING_PAYMENT) {
            throw new BusinessException("Policy is not awaiting payment");
        }
        return policy;
    }

    private void addPolicyMembers(Policy policy, Instant now) {
        CitizenProfile profile = citizenProfileRepository
                .findById(policy.getCitizenProfileId())
                .orElseThrow(() -> new BusinessException("Citizen profile not found"));
        User user = userRepository
                .findById(profile.getUserId())
                .orElseThrow(() -> new BusinessException("User not found"));

        PolicyMember holder = new PolicyMember();
        holder.setId(UUID.randomUUID());
        holder.setPolicyId(policy.getId());
        holder.setMemberType(PolicyMemberType.POLICYHOLDER);
        holder.setFullName(user.getFirstName() + " " + user.getLastName());
        holder.setRelationship("SELF");
        holder.setCreatedAt(now);
        policyMemberRepository.save(holder);

        for (Dependant dependant : dependantRepository.findByCitizenProfileIdOrderByCreatedAtAsc(profile.getId())) {
            PolicyMember member = new PolicyMember();
            member.setId(UUID.randomUUID());
            member.setPolicyId(policy.getId());
            member.setMemberType(PolicyMemberType.DEPENDANT);
            member.setDependantId(dependant.getId());
            member.setFullName(dependant.getFirstName() + " " + dependant.getLastName());
            member.setRelationship(dependant.getRelationship().name());
            member.setCreatedAt(now);
            policyMemberRepository.save(member);
        }
    }

    private void createInitialPremiumSchedule(Policy policy, Instant now) {
        PremiumSchedule schedule = new PremiumSchedule();
        schedule.setId(UUID.randomUUID());
        schedule.setPolicyId(policy.getId());
        schedule.setDueDate(LocalDate.now());
        schedule.setAmount(policy.getPremiumAmount());
        schedule.setStatus(PremiumScheduleStatus.PENDING);
        schedule.setCreatedAt(now);
        premiumScheduleRepository.save(schedule);
    }

    private PolicyResponse toResponse(Policy policy) {
        List<PolicyResponse.PolicyMemberResponse> members =
                policyMemberRepository.findByPolicyIdOrderByCreatedAtAsc(policy.getId()).stream()
                        .map(m -> PolicyResponse.PolicyMemberResponse.builder()
                                .id(m.getId())
                                .memberType(m.getMemberType())
                                .fullName(m.getFullName())
                                .relationship(m.getRelationship())
                                .build())
                        .toList();

        return PolicyResponse.builder()
                .id(policy.getId())
                .policyNumber(policy.getPolicyNumber())
                .organizationId(policy.getOrganizationId())
                .applicationId(policy.getApplicationId())
                .citizenProfileId(policy.getCitizenProfileId())
                .productPlanId(policy.getProductPlanId())
                .status(policy.getStatus())
                .premiumAmount(policy.getPremiumAmount())
                .premiumFrequency(policy.getPremiumFrequency())
                .startDate(policy.getStartDate())
                .endDate(policy.getEndDate())
                .qrVerificationToken(policy.getQrVerificationToken())
                .members(members)
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    private void assertCanAccessPolicy(Policy policy) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            return;
        }
        if (user.hasRole(RoleCodes.UNDERWRITER)
                || user.hasRole(RoleCodes.PARTNER_ADMIN)
                || user.hasRole(RoleCodes.CLAIMS_OFFICER)
                || user.hasRole(RoleCodes.CLAIMS_SUPERVISOR)) {
            if (policy.getOrganizationId().equals(user.getOrganizationId())) {
                return;
            }
        }
        CitizenProfile profile = citizenProfileRepository
                .findByUserId(user.getUserId())
                .orElseThrow(() -> new BusinessException("Access denied"));
        if (policy.getCitizenProfileId().equals(profile.getId())) {
            return;
        }
        throw new BusinessException("Access denied to this policy");
    }

    private CitizenProfile requireMyProfile() {
        return citizenProfileRepository
                .findByUserId(SecurityUtils.currentUser().getUserId())
                .orElseThrow(() -> new BusinessException("Citizen profile not found"));
    }

    private UUID requireTenantOrganizationId() {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (user.getOrganizationId() == null) {
            throw new BusinessException("No organization associated with this account");
        }
        return user.getOrganizationId();
    }

    private String generatePolicyNumber() {
        return "POL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
