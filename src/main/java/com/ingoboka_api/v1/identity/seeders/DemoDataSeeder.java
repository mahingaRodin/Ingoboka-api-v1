package com.ingoboka_api.v1.identity.seeders;

import com.ingoboka_api.v1.claim.models.Claim;
import com.ingoboka_api.v1.claim.models.ClaimStatusHistory;
import com.ingoboka_api.v1.claim.repositories.ClaimRepository;
import com.ingoboka_api.v1.claim.repositories.ClaimStatusHistoryRepository;
import com.ingoboka_api.v1.common.enums.ApplicationStatus;
import com.ingoboka_api.v1.common.enums.ClaimStatus;
import com.ingoboka_api.v1.common.enums.ConsentType;
import com.ingoboka_api.v1.common.enums.KycStatus;
import com.ingoboka_api.v1.common.enums.OrganizationType;
import com.ingoboka_api.v1.common.enums.PolicyStatus;
import com.ingoboka_api.v1.common.enums.PremiumFrequency;
import com.ingoboka_api.v1.common.enums.ProductStatus;
import com.ingoboka_api.v1.common.enums.UserStatus;
import com.ingoboka_api.v1.customer.models.CitizenProfile;
import com.ingoboka_api.v1.customer.models.Consent;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.customer.repositories.ConsentRepository;
import com.ingoboka_api.v1.enrollment.models.PolicyApplication;
import com.ingoboka_api.v1.enrollment.repositories.PolicyApplicationRepository;
import com.ingoboka_api.v1.identity.models.Organization;
import com.ingoboka_api.v1.identity.models.Role;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.repositories.OrganizationRepository;
import com.ingoboka_api.v1.identity.repositories.RoleRepository;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.identity.services.OrganizationManagementService;
import com.ingoboka_api.v1.partner.models.PartnerProfile;
import com.ingoboka_api.v1.partner.repositories.PartnerProfileRepository;
import com.ingoboka_api.v1.policy.models.Policy;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import com.ingoboka_api.v1.product.models.InsuranceProduct;
import com.ingoboka_api.v1.product.models.ProductBenefit;
import com.ingoboka_api.v1.product.models.ProductExclusion;
import com.ingoboka_api.v1.product.models.ProductFaq;
import com.ingoboka_api.v1.product.models.ProductPlan;
import com.ingoboka_api.v1.product.repositories.InsuranceProductRepository;
import com.ingoboka_api.v1.product.repositories.ProductBenefitRepository;
import com.ingoboka_api.v1.product.repositories.ProductExclusionRepository;
import com.ingoboka_api.v1.product.repositories.ProductFaqRepository;
import com.ingoboka_api.v1.product.repositories.ProductPlanRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Profile("docker")
@RequiredArgsConstructor
public class DemoDataSeeder implements ApplicationRunner {

    private static final String DEMO_ORG_CODE = "DEMO_INSURER";
    private static final String DEMO_PASSWORD = "Ingoboka@2026";

    private final OrganizationRepository organizationRepository;
    private final OrganizationManagementService organizationManagementService;
    private final PartnerProfileRepository partnerProfileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CitizenProfileRepository citizenProfileRepository;
    private final ConsentRepository consentRepository;
    private final InsuranceProductRepository productRepository;
    private final ProductPlanRepository planRepository;
    private final ProductBenefitRepository benefitRepository;
    private final ProductExclusionRepository exclusionRepository;
    private final ProductFaqRepository productFaqRepository;
    private final PolicyApplicationRepository applicationRepository;
    private final PolicyRepository policyRepository;
    private final ClaimRepository claimRepository;
    private final ClaimStatusHistoryRepository claimStatusHistoryRepository;

    @Value("${ingoboka.seed.demo.enabled:true}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            return;
        }
        if (organizationRepository.findByCode(DEMO_ORG_CODE).isPresent()) {
            return;
        }

        Instant now = Instant.now();
        Organization org = organizationManagementService.createOrganization(
                "Demo Insurer Ltd", DEMO_ORG_CODE, OrganizationType.INSURER);
        savePartnerProfile(org.getId(), now);

        User partnerAdmin = saveStaff(
                org,
                "eric@demo-insurer.rw",
                "+250788000001",
                "Eric",
                "Mukamana",
                RoleCodes.PARTNER_ADMIN,
                now);
        saveStaff(
                org,
                "claims@demo-insurer.rw",
                "+250788000002",
                "Claire",
                "Uwase",
                RoleCodes.CLAIMS_OFFICER,
                now);
        saveStaff(
                org,
                "agent@demo-insurer.rw",
                "+250788000099",
                "Alex",
                "Agent",
                RoleCodes.AGENT,
                now);

        User citizen = saveCitizen(now);
        CitizenProfile profile = citizenProfileRepository
                .findByUserId(citizen.getId())
                .orElseThrow();

        Consent consent = saveConsent(citizen.getId(), now);
        InsuranceProduct product = saveProduct(org.getId(), now);
        ProductPlan monthlyPlan = savePlan(product.getId(), "PA-MONTHLY", "Monthly Plan", PremiumFrequency.MONTHLY, 500, now);
        savePlan(product.getId(), "PA-DAILY", "Daily Plan", PremiumFrequency.DAILY, 150, now);
        savePlan(product.getId(), "PA-WEEKLY", "Weekly Plan", PremiumFrequency.WEEKLY, 350, now);
        saveProductFaq(product.getId(), now);

        PolicyApplication application = saveApplication(profile, org.getId(), monthlyPlan, consent.getId(), now);
        Policy policy = savePolicy(application, profile, monthlyPlan, now);
        saveClaim(policy, profile, org.getId(), now);

        log.info(
                "Seeded demo data: insurer={}, citizen=+250780000001, partnerAdmin={}",
                DEMO_ORG_CODE,
                partnerAdmin.getEmail());
    }

    private void savePartnerProfile(UUID orgId, Instant now) {
        PartnerProfile profile = new PartnerProfile();
        profile.setId(UUID.randomUUID());
        profile.setOrganizationId(orgId);
        profile.setRegistrationNumber("DEMO-REG-001");
        profile.setContactEmail("contact@demo-insurer.rw");
        profile.setContactPhone("+250788000000");
        profile.setCountry("RW");
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        partnerProfileRepository.save(profile);
    }

    private User saveStaff(
            Organization org,
            String email,
            String phone,
            String firstName,
            String lastName,
            String roleCode,
            Instant now) {
        if (userRepository.existsByEmailIgnoreCase(email)) {
            return userRepository.findByEmailIgnoreCase(email).orElseThrow();
        }

        Role role = roleRepository
                .findByCode(roleCode)
                .orElseThrow(() -> new IllegalStateException("Role missing: " + roleCode));

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setOrganization(org);
        user.setEmail(email.trim().toLowerCase());
        user.setPhoneNumber(phone);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        user.setMustChangePassword(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    private User saveCitizen(Instant now) {
        String phone = "+250780000001";
        if (userRepository.existsByPhoneNumber(phone)) {
            return userRepository.findByPhoneNumber(phone).orElseThrow();
        }

        Role citizenRole = roleRepository
                .findByCode(RoleCodes.CITIZEN)
                .orElseThrow(() -> new IllegalStateException("CITIZEN role missing"));

        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("citizen.demo@ingoboka.rw");
        user.setPhoneNumber(phone);
        user.setFirstName("Jean");
        user.setLastName("Uwimana");
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setPhoneVerified(true);
        user.setMustChangePassword(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.getRoles().add(citizenRole);
        userRepository.save(user);

        CitizenProfile profile = new CitizenProfile();
        profile.setId(UUID.randomUUID());
        profile.setUserId(user.getId());
        profile.setDateOfBirth(LocalDate.of(1995, 3, 15));
        profile.setCountry("RW");
        profile.setOccupation("Moto taxi rider");
        profile.setPreferredLanguage("en");
        profile.setKycStatus(KycStatus.VERIFIED);
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);
        citizenProfileRepository.save(profile);
        return user;
    }

    private Consent saveConsent(UUID userId, Instant now) {
        Consent consent = new Consent();
        consent.setId(UUID.randomUUID());
        consent.setUserId(userId);
        consent.setConsentType(ConsentType.TERMS_OF_SERVICE);
        consent.setVersion("1.0");
        consent.setGranted(true);
        consent.setGrantedAt(now);
        consent.setCreatedAt(now);
        return consentRepository.save(consent);
    }

    private InsuranceProduct saveProduct(UUID orgId, Instant now) {
        InsuranceProduct product = new InsuranceProduct();
        product.setId(UUID.randomUUID());
        product.setOrganizationId(orgId);
        product.setCode("PA-MICRO");
        product.setName("Personal Accident Micro");
        product.setDescription("Affordable accident protection for informal workers.");
        product.setCategory("PERSONAL_ACCIDENT");
        product.setStatus(ProductStatus.PUBLISHED);
        product.setPublishedAt(now);
        product.setCreatedAt(now);
        product.setUpdatedAt(now);
        return productRepository.save(product);
    }

    private ProductPlan savePlan(
            UUID productId,
            String code,
            String name,
            PremiumFrequency frequency,
            int premium,
            Instant now) {
        ProductPlan plan = new ProductPlan();
        plan.setId(UUID.randomUUID());
        plan.setProductId(productId);
        plan.setCode(code);
        plan.setName(name);
        plan.setDescription("Demo " + name.toLowerCase());
        plan.setPremiumAmount(BigDecimal.valueOf(premium));
        plan.setPremiumFrequency(frequency);
        plan.setWaitingPeriodDays(30);
        plan.setStatus(ProductStatus.PUBLISHED);
        plan.setCreatedAt(now);
        plan.setUpdatedAt(now);
        planRepository.save(plan);

        ProductBenefit benefit = new ProductBenefit();
        benefit.setId(UUID.randomUUID());
        benefit.setPlanId(plan.getId());
        benefit.setTitle("Accident Protection");
        benefit.setDescription("Lump sum benefit for accidental injury or disability.");
        benefit.setCoverageLimit(BigDecimal.valueOf(500_000));
        benefit.setSortOrder(0);
        benefit.setCreatedAt(now);
        benefitRepository.save(benefit);

        ProductExclusion exclusion = new ProductExclusion();
        exclusion.setId(UUID.randomUUID());
        exclusion.setPlanId(plan.getId());
        exclusion.setTitle("Illegal activities");
        exclusion.setDescription("Injuries resulting from illegal activities.");
        exclusion.setSortOrder(0);
        exclusion.setCreatedAt(now);
        exclusionRepository.save(exclusion);
        return plan;
    }

    private void saveProductFaq(UUID productId, Instant now) {
        ProductFaq faq = new ProductFaq();
        faq.setId(UUID.randomUUID());
        faq.setProductId(productId);
        faq.setQuestion("When does my cover start?");
        faq.setAnswer("Full cover begins after a 30-day waiting period from your first successful payment.");
        faq.setSortOrder(0);
        faq.setCreatedAt(now);
        productFaqRepository.save(faq);
    }

    private PolicyApplication saveApplication(
            CitizenProfile profile,
            UUID orgId,
            ProductPlan plan,
            UUID consentId,
            Instant now) {
        PolicyApplication application = new PolicyApplication();
        application.setId(UUID.randomUUID());
        application.setApplicationReference("APP-DEMO-0001");
        application.setCitizenProfileId(profile.getId());
        application.setOrganizationId(orgId);
        application.setProductPlanId(plan.getId());
        application.setConsentId(consentId);
        application.setPremiumAmount(plan.getPremiumAmount());
        application.setPremiumFrequency(plan.getPremiumFrequency());
        application.setStatus(ApplicationStatus.APPROVED);
        application.setSubmittedAt(now);
        application.setReviewedAt(now);
        application.setCreatedAt(now);
        application.setUpdatedAt(now);
        return applicationRepository.save(application);
    }

    private Policy savePolicy(
            PolicyApplication application, CitizenProfile profile, ProductPlan plan, Instant now) {
        Policy policy = new Policy();
        policy.setId(UUID.randomUUID());
        policy.setPolicyNumber("ING-DEMO-0001");
        policy.setOrganizationId(application.getOrganizationId());
        policy.setApplicationId(application.getId());
        policy.setCitizenProfileId(profile.getId());
        policy.setProductPlanId(plan.getId());
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setPremiumAmount(plan.getPremiumAmount());
        policy.setPremiumFrequency(plan.getPremiumFrequency());
        policy.setStartDate(LocalDate.now().minusMonths(1));
        policy.setEndDate(LocalDate.now().plusMonths(11));
        policy.setQrVerificationToken("demo-verify-token-0001");
        policy.setCreatedAt(now);
        policy.setUpdatedAt(now);
        return policyRepository.save(policy);
    }

    private void saveClaim(Policy policy, CitizenProfile profile, UUID orgId, Instant now) {
        Claim claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setClaimNumber("CLM-DEMO-0001");
        claim.setPolicyId(policy.getId());
        claim.setOrganizationId(orgId);
        claim.setCitizenProfileId(profile.getId());
        claim.setClaimType("MEDICAL");
        claim.setDescription("Outpatient medical visit after minor accident.");
        claim.setClaimedAmount(BigDecimal.valueOf(75_000));
        claim.setStatus(ClaimStatus.UNDER_REVIEW);
        claim.setCreatedAt(now.minusSeconds(86_400));
        claim.setUpdatedAt(now);
        claimRepository.save(claim);

        ClaimStatusHistory submitted = new ClaimStatusHistory();
        submitted.setId(UUID.randomUUID());
        submitted.setClaimId(claim.getId());
        submitted.setFromStatus(null);
        submitted.setToStatus(ClaimStatus.SUBMITTED);
        submitted.setReason("Claim submitted");
        submitted.setCreatedAt(now.minusSeconds(86_400));
        claimStatusHistoryRepository.save(submitted);

        ClaimStatusHistory review = new ClaimStatusHistory();
        review.setId(UUID.randomUUID());
        review.setClaimId(claim.getId());
        review.setFromStatus(ClaimStatus.SUBMITTED);
        review.setToStatus(ClaimStatus.UNDER_REVIEW);
        review.setReason("Assigned to officer");
        review.setCreatedAt(now.minusSeconds(43_200));
        claimStatusHistoryRepository.save(review);
    }
}
