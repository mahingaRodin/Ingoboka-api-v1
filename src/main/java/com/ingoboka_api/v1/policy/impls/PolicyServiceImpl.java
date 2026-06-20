package com.ingoboka_api.v1.policy.impls;

import com.ingoboka_api.v1.common.enums.DocumentAccessClassification;
import com.ingoboka_api.v1.common.enums.PolicyMemberType;
import com.ingoboka_api.v1.common.enums.PolicyStatus;
import com.ingoboka_api.v1.common.enums.PremiumScheduleStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.AttachPolicyDocumentRequest;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.PolicyActivityResponse;
import com.ingoboka_api.v1.common.responses.PolicyCardResponse;
import com.ingoboka_api.v1.common.responses.PolicyResponse;
import com.ingoboka_api.v1.common.responses.PolicyVerificationResponse;
import com.ingoboka_api.v1.common.responses.PremiumScheduleResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.common.util.PaginationUtils;
import com.ingoboka_api.v1.customer.models.CitizenProfile;
import com.ingoboka_api.v1.customer.models.Dependant;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.customer.repositories.DependantRepository;
import com.ingoboka_api.v1.claim.models.Claim;
import com.ingoboka_api.v1.claim.repositories.ClaimRepository;
import com.ingoboka_api.v1.enrollment.models.PolicyApplication;
import com.ingoboka_api.v1.identity.models.Organization;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.identity.services.OrganizationManagementService;
import com.ingoboka_api.v1.billing.models.PremiumSchedule;
import com.ingoboka_api.v1.billing.repositories.PremiumScheduleRepository;
import com.ingoboka_api.v1.billing.services.BillingFinanceService;
import com.ingoboka_api.v1.product.models.InsuranceProduct;
import com.ingoboka_api.v1.product.models.ProductBenefit;
import com.ingoboka_api.v1.product.models.ProductPlan;
import com.ingoboka_api.v1.product.repositories.InsuranceProductRepository;
import com.ingoboka_api.v1.product.repositories.ProductBenefitRepository;
import com.ingoboka_api.v1.product.repositories.ProductPlanRepository;
import com.ingoboka_api.v1.policy.models.Policy;
import com.ingoboka_api.v1.policy.models.PolicyDocument;
import com.ingoboka_api.v1.policy.models.PolicyMember;
import com.ingoboka_api.v1.policy.repositories.PolicyDocumentRepository;
import com.ingoboka_api.v1.policy.repositories.PolicyMemberRepository;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import com.ingoboka_api.v1.policy.services.PolicyIssuanceService;
import com.ingoboka_api.v1.policy.services.PolicyService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
    private final BillingFinanceService billingFinanceService;
    private final ProductPlanRepository productPlanRepository;
    private final InsuranceProductRepository insuranceProductRepository;
    private final ProductBenefitRepository productBenefitRepository;
    private final ClaimRepository claimRepository;

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
    public PageResponse<PolicyResponse> listMyPolicies(int page, int size) {
        CitizenProfile profile = requireMyProfile();
        Page<Policy> result = policyRepository.findByCitizenProfileIdOrderByCreatedAtDesc(
                profile.getId(), PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PolicyResponse> listTenantPolicies(int page, int size) {
        UUID orgId = requireTenantOrganizationId();
        Page<Policy> result = policyRepository.findByOrganizationIdOrderByCreatedAtDesc(
                orgId, PaginationUtils.toPageable(page, size));
        return PageResponse.from(result.map(this::toResponse));
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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PremiumScheduleResponse> listPremiumSchedules(UUID policyId, int page, int size) {
        Policy policy = policyRepository
                .findById(policyId)
                .orElseThrow(() -> new BusinessException("Policy not found"));
        assertCanAccessPolicy(policy);
        var schedules = premiumScheduleRepository.findByPolicyIdOrderByDueDateAsc(policyId);
        int from = Math.min(page * size, schedules.size());
        int to = Math.min(from + size, schedules.size());
        var slice = schedules.subList(from, to).stream().map(this::toScheduleResponse).toList();
        return PageResponse.<PremiumScheduleResponse>builder()
                .content(slice)
                .page(page)
                .size(size)
                .totalElements(schedules.size())
                .totalPages(size == 0 ? 0 : (int) Math.ceil((double) schedules.size() / size))
                .first(page == 0)
                .last(to >= schedules.size())
                .build();
    }

    private PremiumScheduleResponse toScheduleResponse(PremiumSchedule schedule) {
        return PremiumScheduleResponse.builder()
                .id(schedule.getId())
                .policyId(schedule.getPolicyId())
                .dueDate(schedule.getDueDate())
                .amount(schedule.getAmount())
                .status(schedule.getStatus())
                .paidAt(schedule.getPaidAt())
                .paymentId(schedule.getPaymentId())
                .build();
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
        billingFinanceService.issueBillForSchedule(schedule, policy.getOrganizationId(), policy.getId());
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
                .productName(resolveProductName(policy.getProductPlanId()))
                .insurerName(resolveInsurerName(policy.getOrganizationId()))
                .coverageAmount(resolveCoverageAmount(policy.getProductPlanId()))
                .currency("RWF")
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    private String resolveProductName(UUID planId) {
        return productPlanRepository
                .findById(planId)
                .flatMap(plan -> insuranceProductRepository.findById(plan.getProductId()))
                .map(InsuranceProduct::getName)
                .orElse("Insurance Product");
    }

    private String resolveInsurerName(UUID organizationId) {
        return organizationManagementService
                .findById(organizationId)
                .map(Organization::getName)
                .orElse("Partner Insurer");
    }

    private BigDecimal resolveCoverageAmount(UUID planId) {
        return productBenefitRepository.findByPlanIdOrderBySortOrderAsc(planId).stream()
                .map(ProductBenefit::getCoverageLimit)
                .filter(limit -> limit != null)
                .max(Comparator.naturalOrder())
                .orElse(null);
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

    @Override
    @Transactional(readOnly = true)
    public PolicyCardResponse getPolicyCard(UUID policyId) {
        Policy policy = policyRepository
                .findById(policyId)
                .orElseThrow(() -> new BusinessException("Policy not found"));
        assertCanAccessPolicy(policy);
        User holder = userRepository
                .findById(citizenProfileRepository
                        .findById(policy.getCitizenProfileId())
                        .orElseThrow(() -> new BusinessException("Profile not found"))
                        .getUserId())
                .orElseThrow(() -> new BusinessException("Policy holder not found"));
        return PolicyCardResponse.builder()
                .policyId(policy.getId())
                .policyNumber(policy.getPolicyNumber())
                .holderName(holder.getFirstName() + " " + holder.getLastName())
                .productName(resolveProductName(policy.getProductPlanId()))
                .status(policy.getStatus())
                .premium(policy.getPremiumAmount())
                .startDate(policy.getStartDate())
                .endDate(policy.getEndDate())
                .qrToken(policy.getQrVerificationToken())
                .verificationUrl("/api/v1/verify/" + policy.getQrVerificationToken())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PolicyActivityResponse> listMyActivity(int page, int size) {
        CitizenProfile profile = requireMyProfile();
        List<PolicyActivityResponse> events = new ArrayList<>();
        policyRepository.findByCitizenProfileIdOrderByCreatedAtDesc(profile.getId()).forEach(policy -> {
            if (policy.getStatus() == PolicyStatus.ACTIVE) {
                events.add(PolicyActivityResponse.builder()
                        .type("POLICY_ACTIVATED")
                        .label("Policy activated — " + resolveProductName(policy.getProductPlanId()))
                        .occurredAt(policy.getCreatedAt())
                        .policyId(policy.getId())
                        .build());
            }
        });
        claimRepository.findByCitizenProfileIdOrderByCreatedAtDesc(profile.getId()).forEach(claim -> events.add(
                PolicyActivityResponse.builder()
                        .type("CLAIM_SUBMITTED")
                        .label("Claim " + claim.getClaimNumber() + " submitted")
                        .occurredAt(claim.getCreatedAt())
                        .policyId(claim.getPolicyId())
                        .claimId(claim.getId())
                        .build()));
        events.sort(Comparator.comparing(PolicyActivityResponse::getOccurredAt).reversed());
        int from = Math.min(page * size, events.size());
        int to = Math.min(from + size, events.size());
        List<PolicyActivityResponse> slice = events.subList(from, to);
        return PageResponse.<PolicyActivityResponse>builder()
                .content(slice)
                .page(page)
                .size(size)
                .totalElements(events.size())
                .totalPages(size == 0 ? 0 : (int) Math.ceil((double) events.size() / size))
                .first(page == 0)
                .last(to >= events.size())
                .build();
    }
}
