package com.ingoboka_api.v1.enrollment.impls;

import com.ingoboka_api.v1.common.enums.ApplicationStatus;
import com.ingoboka_api.v1.common.enums.QuoteStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.enums.ConsentType;
import com.ingoboka_api.v1.common.requests.GenerateQuoteRequest;
import com.ingoboka_api.v1.common.requests.GrantConsentRequest;
import com.ingoboka_api.v1.common.requests.NeedsAssessmentRequest;
import com.ingoboka_api.v1.common.requests.QuickApplicationRequest;
import com.ingoboka_api.v1.common.requests.ReviewApplicationRequest;
import com.ingoboka_api.v1.common.requests.SubmitApplicationRequest;
import com.ingoboka_api.v1.common.enums.ProductStatus;
import com.ingoboka_api.v1.common.responses.ApplicationResponse;
import com.ingoboka_api.v1.common.responses.RecommendedProductResponse;
import com.ingoboka_api.v1.common.responses.NeedsAssessmentResponse;
import com.ingoboka_api.v1.common.responses.PageResponse;
import com.ingoboka_api.v1.common.responses.QuoteResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.common.util.PaginationUtils;
import com.ingoboka_api.v1.customer.models.CitizenProfile;
import com.ingoboka_api.v1.customer.models.Consent;
import com.ingoboka_api.v1.customer.repositories.ConsentRepository;
import com.ingoboka_api.v1.customer.services.CustomerProfileService;
import com.ingoboka_api.v1.enrollment.models.ApplicationAnswer;
import com.ingoboka_api.v1.enrollment.models.PolicyApplication;
import com.ingoboka_api.v1.enrollment.models.Quote;
import com.ingoboka_api.v1.enrollment.models.QuoteAnswer;
import com.ingoboka_api.v1.enrollment.repositories.ApplicationAnswerRepository;
import com.ingoboka_api.v1.enrollment.repositories.PolicyApplicationRepository;
import com.ingoboka_api.v1.enrollment.repositories.QuoteAnswerRepository;
import com.ingoboka_api.v1.enrollment.repositories.QuoteRepository;
import com.ingoboka_api.v1.enrollment.services.EnrollmentService;
import com.ingoboka_api.v1.identity.models.RoleCodes;
import com.ingoboka_api.v1.policy.services.PolicyIssuanceService;
import com.ingoboka_api.v1.product.models.EligibilityRule;
import com.ingoboka_api.v1.product.models.InsuranceProduct;
import com.ingoboka_api.v1.product.models.ProductPlan;
import com.ingoboka_api.v1.product.repositories.EligibilityRuleRepository;
import com.ingoboka_api.v1.product.repositories.InsuranceProductRepository;
import com.ingoboka_api.v1.product.repositories.ProductPlanRepository;
import com.ingoboka_api.v1.product.services.ProductCatalogService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl implements EnrollmentService {

    private static final int QUOTE_VALIDITY_DAYS = 7;

    private final QuoteRepository quoteRepository;
    private final QuoteAnswerRepository quoteAnswerRepository;
    private final PolicyApplicationRepository applicationRepository;
    private final ApplicationAnswerRepository applicationAnswerRepository;
    private final CustomerProfileService customerProfileService;
    private final ProductCatalogService productCatalogService;
    private final InsuranceProductRepository productRepository;
    private final ProductPlanRepository productPlanRepository;
    private final EligibilityRuleRepository eligibilityRuleRepository;
    private final PolicyIssuanceService policyIssuanceService;
    private final UserRepository userRepository;
    private final AuditComplianceService auditComplianceService;
    private final ConsentRepository consentRepository;

    @Value("${ingoboka.enrollment.sandbox-auto-approve:true}")
    private boolean sandboxAutoApprove;

    @Override
    @Transactional
    public QuoteResponse generateQuote(GenerateQuoteRequest request) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        CitizenProfile profile = customerProfileService.requireProfileForUser(user.getUserId());

        ProductPlan plan = productCatalogService.requirePublishedPlan(request.getProductPlanId());
        InsuranceProduct product = productRepository
                .findById(plan.getProductId())
                .orElseThrow(() -> new BusinessException("Product not found"));

        validateEligibility(profile, plan.getId());

        Instant now = Instant.now();
        Quote quote = new Quote();
        quote.setId(UUID.randomUUID());
        quote.setQuoteReference(generateReference("QT"));
        quote.setCitizenProfileId(profile.getId());
        quote.setOrganizationId(product.getOrganizationId());
        quote.setProductPlanId(plan.getId());
        quote.setPremiumAmount(plan.getPremiumAmount());
        quote.setPremiumFrequency(plan.getPremiumFrequency());
        quote.setValidUntil(now.plus(QUOTE_VALIDITY_DAYS, ChronoUnit.DAYS));
        quote.setStatus(QuoteStatus.ACTIVE);
        quote.setCreatedAt(now);
        quoteRepository.save(quote);

        saveAnswers(request.getAnswers() != null ? request.getAnswers() : Map.of(), quote.getId(), now);
        return toQuoteResponse(quote, request.getAnswers());
    }

    @Override
    @Transactional
    public ApplicationResponse createQuickApplication(QuickApplicationRequest request) {
        GenerateQuoteRequest quoteRequest = new GenerateQuoteRequest();
        quoteRequest.setProductPlanId(request.getProductPlanId());
        quoteRequest.setAnswers(Map.of());
        QuoteResponse quote = generateQuote(quoteRequest);

        GrantConsentRequest consentRequest = new GrantConsentRequest();
        consentRequest.setConsentType(ConsentType.DATA_PROCESSING);
        consentRequest.setVersion("1.0");
        var consent = grantConsentForUser(
                SecurityUtils.currentUser().getUserId(), consentRequest);

        SubmitApplicationRequest submitRequest = new SubmitApplicationRequest();
        submitRequest.setQuoteId(quote.getId());
        submitRequest.setConsentId(consent.getId());
        ApplicationResponse application = submitApplication(submitRequest);
        return getApplication(application.getId());
    }

    @Override
    @Transactional
    public ApplicationResponse submitApplicationById(UUID applicationId) {
        PolicyApplication application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new BusinessException("Application not found"));
        assertCanAccessApplication(application);
        autoApproveIfSandbox(application);
        return toApplicationResponse(application, loadAnswers(application.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public NeedsAssessmentResponse assessNeeds(NeedsAssessmentRequest request) {
        int score = 50;
        if (request.getDependents() != null && request.getDependents() > 2) {
            score += 15;
        }
        if (request.getPrimaryRisk() != null) {
            score += 10;
        }
        List<String> categories = new ArrayList<>();
        categories.add("PERSONAL_ACCIDENT");
        if (score >= 65) {
            categories.add("HEALTH_MICRO");
        }
        final int assessmentScore = score;
        List<RecommendedProductResponse> recommendedProducts = productRepository
                .findByStatusOrderByPublishedAtDesc(ProductStatus.PUBLISHED)
                .stream()
                .filter(product -> categories.contains(product.getCategory()))
                .limit(3)
                .map(product -> {
                    var plans = productPlanRepository.findByProductIdAndStatus(product.getId(), ProductStatus.PUBLISHED);
                    var startingPremium = plans.stream()
                            .map(ProductPlan::getPremiumAmount)
                            .min(java.util.Comparator.naturalOrder())
                            .orElse(null);
                    return RecommendedProductResponse.builder()
                            .id(product.getId())
                            .name(product.getName())
                            .category(product.getCategory())
                            .startingPremium(startingPremium)
                            .currency("RWF")
                            .matchScore(Math.min(100, assessmentScore + 20))
                            .reason("Matches " + product.getCategory().replace('_', ' ').toLowerCase())
                            .build();
                })
                .toList();
        return NeedsAssessmentResponse.builder()
                .score(score)
                .guidance("Based on your profile, consider personal accident cover first.")
                .recommendedCategories(categories)
                .recommendedProducts(recommendedProducts)
                .build();
    }

    @Override
    @Transactional
    public ApplicationResponse createAgentAssistedApplication(String citizenPhone, UUID productPlanId) {
        var citizen = userRepository
                .findByPhoneNumber(citizenPhone)
                .orElseThrow(() -> new BusinessException("Citizen not found for phone " + citizenPhone));
        auditComplianceService.log(
                "AGENT_ASSISTED_ENROLLMENT",
                "USER",
                citizen.getId(),
                "Agent assisted enrollment for " + citizenPhone);
        CitizenProfile profile = customerProfileService.requireProfileForUser(citizen.getId());
        QuoteResponse quote = generateQuoteForProfile(profile, productPlanId);
        GrantConsentRequest consentRequest = new GrantConsentRequest();
        consentRequest.setConsentType(ConsentType.DATA_PROCESSING);
        consentRequest.setVersion("1.0");
        Consent consent = grantConsentForUser(citizen.getId(), consentRequest);
        PolicyApplication application = buildApplication(profile, quote, consent.getId());
        autoApproveIfSandbox(application);
        return toApplicationResponse(application, loadAnswers(application.getId()));
    }

    private void autoApproveIfSandbox(PolicyApplication application) {
        if (!sandboxAutoApprove || application.getStatus() == ApplicationStatus.APPROVED) {
            return;
        }
        application.setStatus(ApplicationStatus.APPROVED);
        application.setDecisionReason("Sandbox auto-approval");
        application.setReviewedAt(Instant.now());
        application.setUpdatedAt(Instant.now());
        applicationRepository.save(application);
        policyIssuanceService.issueFromApprovedApplication(application);
    }

    private QuoteResponse generateQuoteForProfile(CitizenProfile profile, UUID productPlanId) {
        GenerateQuoteRequest request = new GenerateQuoteRequest();
        request.setProductPlanId(productPlanId);
        request.setAnswers(Map.of());
        ProductPlan plan = productCatalogService.requirePublishedPlan(productPlanId);
        InsuranceProduct product = productRepository
                .findById(plan.getProductId())
                .orElseThrow(() -> new BusinessException("Product not found"));
        validateEligibility(profile, plan.getId());
        Instant now = Instant.now();
        Quote quote = new Quote();
        quote.setId(UUID.randomUUID());
        quote.setQuoteReference(generateReference("QT"));
        quote.setCitizenProfileId(profile.getId());
        quote.setOrganizationId(product.getOrganizationId());
        quote.setProductPlanId(plan.getId());
        quote.setPremiumAmount(plan.getPremiumAmount());
        quote.setPremiumFrequency(plan.getPremiumFrequency());
        quote.setValidUntil(now.plus(QUOTE_VALIDITY_DAYS, ChronoUnit.DAYS));
        quote.setStatus(QuoteStatus.ACTIVE);
        quote.setCreatedAt(now);
        quoteRepository.save(quote);
        return toQuoteResponse(quote, Map.of());
    }

    private PolicyApplication buildApplication(CitizenProfile profile, QuoteResponse quote, UUID consentId) {
        Quote quoteEntity = quoteRepository.findById(quote.getId()).orElseThrow();
        Instant now = Instant.now();
        PolicyApplication application = new PolicyApplication();
        application.setId(UUID.randomUUID());
        application.setApplicationReference(generateReference("APP"));
        application.setQuoteId(quote.getId());
        application.setCitizenProfileId(profile.getId());
        application.setOrganizationId(quoteEntity.getOrganizationId());
        application.setProductPlanId(quoteEntity.getProductPlanId());
        application.setConsentId(consentId);
        application.setPremiumAmount(quoteEntity.getPremiumAmount());
        application.setPremiumFrequency(quoteEntity.getPremiumFrequency());
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setSubmittedAt(now);
        application.setCreatedAt(now);
        application.setUpdatedAt(now);
        applicationRepository.save(application);
        quoteEntity.setStatus(QuoteStatus.CONVERTED);
        quoteRepository.save(quoteEntity);
        return application;
    }

    private Consent grantConsentForUser(UUID userId, GrantConsentRequest request) {
        Instant now = Instant.now();
        Consent consent = new Consent();
        consent.setId(UUID.randomUUID());
        consent.setUserId(userId);
        consent.setConsentType(request.getConsentType());
        consent.setVersion(request.getVersion());
        consent.setGranted(true);
        consent.setGrantedAt(now);
        consent.setCreatedAt(now);
        return consentRepository.save(consent);
    }

    @Override
    @Transactional
    public ApplicationResponse submitApplication(SubmitApplicationRequest request) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        CitizenProfile profile = customerProfileService.requireProfileForUser(user.getUserId());
        Consent consent = customerProfileService.requireActiveConsent(user.getUserId(), request.getConsentId());

        Quote quote = quoteRepository
                .findById(request.getQuoteId())
                .orElseThrow(() -> new BusinessException("Quote not found"));
        if (!quote.getCitizenProfileId().equals(profile.getId())) {
            throw new BusinessException("Quote does not belong to this citizen");
        }
        if (quote.getStatus() != QuoteStatus.ACTIVE) {
            throw new BusinessException("Quote is no longer active");
        }
        if (quote.getValidUntil().isBefore(Instant.now())) {
            quote.setStatus(QuoteStatus.EXPIRED);
            quoteRepository.save(quote);
            throw new BusinessException("Quote has expired");
        }

        Instant now = Instant.now();
        PolicyApplication application = new PolicyApplication();
        application.setId(UUID.randomUUID());
        application.setApplicationReference(generateReference("APP"));
        application.setQuoteId(quote.getId());
        application.setCitizenProfileId(profile.getId());
        application.setOrganizationId(quote.getOrganizationId());
        application.setProductPlanId(quote.getProductPlanId());
        application.setConsentId(consent.getId());
        application.setPremiumAmount(quote.getPremiumAmount());
        application.setPremiumFrequency(quote.getPremiumFrequency());
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setSubmittedAt(now);
        application.setCreatedAt(now);
        application.setUpdatedAt(now);
        applicationRepository.save(application);

        copyQuoteAnswersToApplication(quote.getId(), application.getId(), now);

        quote.setStatus(QuoteStatus.CONVERTED);
        quoteRepository.save(quote);

        autoApproveIfSandbox(application);
        return toApplicationResponse(
                applicationRepository.findById(application.getId()).orElseThrow(),
                loadAnswers(application.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> listMyApplications(int page, int size) {
        CitizenProfile profile =
                customerProfileService.requireProfileForUser(SecurityUtils.currentUser().getUserId());
        Page<PolicyApplication> result = applicationRepository.findByCitizenProfileIdOrderBySubmittedAtDesc(
                profile.getId(), PaginationUtils.toPageable(page, size, "submittedAt"));
        return PageResponse.from(result.map(
                app -> toApplicationResponse(app, loadAnswers(app.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplication(UUID applicationId) {
        PolicyApplication application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new BusinessException("Application not found"));
        assertCanAccessApplication(application);
        return toApplicationResponse(application, loadAnswers(application.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> listAgentApplications(int page, int size) {
        UUID orgId = requireAgentOrganizationId();
        Page<PolicyApplication> result = applicationRepository.findByOrganizationIdOrderBySubmittedAtDesc(
                orgId, PaginationUtils.toPageable(page, size, "submittedAt"));
        return PageResponse.from(result.map(
                app -> toApplicationResponse(app, loadAnswers(app.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> listTenantApplications(
            ApplicationStatus status, int page, int size) {
        UUID orgId = requireUnderwriterOrganizationId();
        Page<PolicyApplication> result = status == null
                ? applicationRepository.findByOrganizationIdOrderBySubmittedAtDesc(
                        orgId, PaginationUtils.toPageable(page, size, "submittedAt"))
                : applicationRepository.findByOrganizationIdAndStatusOrderBySubmittedAtDesc(
                        orgId, status, PaginationUtils.toPageable(page, size, "submittedAt"));
        return PageResponse.from(result.map(
                app -> toApplicationResponse(app, loadAnswers(app.getId()))));
    }

    @Override
    @Transactional
    public ApplicationResponse reviewApplication(UUID applicationId, ReviewApplicationRequest request) {
        UUID orgId = requireUnderwriterOrganizationId();
        PolicyApplication application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new BusinessException("Application not found"));
        if (!application.getOrganizationId().equals(orgId)) {
            throw new BusinessException("Application is outside your tenant");
        }
        if (request.getStatus() != ApplicationStatus.APPROVED
                && request.getStatus() != ApplicationStatus.REJECTED
                && request.getStatus() != ApplicationStatus.UNDER_REVIEW) {
            throw new BusinessException("Invalid review status");
        }
        if ((request.getStatus() == ApplicationStatus.REJECTED
                        || request.getStatus() == ApplicationStatus.APPROVED)
                && (request.getDecisionReason() == null || request.getDecisionReason().isBlank())) {
            throw new BusinessException("Decision reason is required for approval or rejection");
        }

        IngobokaUserDetails reviewer = SecurityUtils.currentUser();
        application.setStatus(request.getStatus());
        application.setDecisionReason(request.getDecisionReason());
        application.setReviewedBy(reviewer.getUserId());
        application.setReviewedAt(Instant.now());
        application.setUpdatedAt(Instant.now());
        applicationRepository.save(application);

        if (request.getStatus() == ApplicationStatus.APPROVED) {
            policyIssuanceService.issueFromApprovedApplication(application);
        }

        return toApplicationResponse(application, loadAnswers(application.getId()));
    }

    private void validateEligibility(CitizenProfile profile, UUID planId) {
        int age = profile.getDateOfBirth() != null
                ? Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears()
                : 25;
        List<EligibilityRule> rules = eligibilityRuleRepository.findByPlanId(planId);
        for (EligibilityRule rule : rules) {
            if (rule.getMinAge() != null && age < rule.getMinAge()) {
                throw new BusinessException("Applicant does not meet minimum age requirement");
            }
            if (rule.getMaxAge() != null && age > rule.getMaxAge()) {
                throw new BusinessException("Applicant exceeds maximum age for this plan");
            }
        }
    }

    private void saveAnswers(Map<String, String> answers, UUID quoteId, Instant now) {
        for (Map.Entry<String, String> entry : answers.entrySet()) {
            QuoteAnswer answer = new QuoteAnswer();
            answer.setId(UUID.randomUUID());
            answer.setQuoteId(quoteId);
            answer.setQuestionKey(entry.getKey());
            answer.setAnswerValue(entry.getValue());
            answer.setCreatedAt(now);
            quoteAnswerRepository.save(answer);
        }
    }

    private void copyQuoteAnswersToApplication(UUID quoteId, UUID applicationId, Instant now) {
        for (QuoteAnswer quoteAnswer : quoteAnswerRepository.findByQuoteId(quoteId)) {
            ApplicationAnswer answer = new ApplicationAnswer();
            answer.setId(UUID.randomUUID());
            answer.setApplicationId(applicationId);
            answer.setQuestionKey(quoteAnswer.getQuestionKey());
            answer.setAnswerValue(quoteAnswer.getAnswerValue());
            answer.setCreatedAt(now);
            applicationAnswerRepository.save(answer);
        }
    }

    private Map<String, String> loadAnswers(UUID applicationId) {
        Map<String, String> answers = new LinkedHashMap<>();
        applicationAnswerRepository.findByApplicationId(applicationId).forEach(a -> answers.put(a.getQuestionKey(), a.getAnswerValue()));
        return answers;
    }

    private Map<String, String> loadQuoteAnswers(UUID quoteId) {
        Map<String, String> answers = new LinkedHashMap<>();
        quoteAnswerRepository.findByQuoteId(quoteId).forEach(a -> answers.put(a.getQuestionKey(), a.getAnswerValue()));
        return answers;
    }

    private void assertCanAccessApplication(PolicyApplication application) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            return;
        }
        if (user.hasRole(RoleCodes.UNDERWRITER)
                || user.hasRole(RoleCodes.PARTNER_ADMIN)
                || user.hasRole(RoleCodes.INSURER_PRODUCT_MANAGER)) {
            if (application.getOrganizationId().equals(user.getOrganizationId())) {
                return;
            }
        }
        CitizenProfile profile = customerProfileService.requireProfileForUser(user.getUserId());
        if (application.getCitizenProfileId().equals(profile.getId())) {
            return;
        }
        throw new BusinessException("Access denied to this application");
    }

    private UUID requireAgentOrganizationId() {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (!user.hasRole(RoleCodes.AGENT)) {
            throw new AccessDeniedException("Agent role required");
        }
        if (user.getOrganizationId() == null) {
            throw new AccessDeniedException("No organization associated with this agent account");
        }
        return user.getOrganizationId();
    }

    private UUID requireUnderwriterOrganizationId() {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        if (user.getOrganizationId() == null) {
            throw new BusinessException("No organization associated with this account");
        }
        if (!user.hasRole(RoleCodes.UNDERWRITER)
                && !user.hasRole(RoleCodes.PARTNER_ADMIN)
                && !user.hasRole(RoleCodes.PLATFORM_ADMIN)) {
            throw new BusinessException("Only underwriters can review applications");
        }
        return user.getOrganizationId();
    }

    private String generateReference(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private QuoteResponse toQuoteResponse(Quote quote, Map<String, String> answers) {
        ProductPlan plan = productPlanRepository
                .findById(quote.getProductPlanId())
                .orElse(null);
        InsuranceProduct product = plan != null
                ? productRepository.findById(plan.getProductId()).orElse(null)
                : null;
        return QuoteResponse.builder()
                .id(quote.getId())
                .quoteReference(quote.getQuoteReference())
                .citizenProfileId(quote.getCitizenProfileId())
                .organizationId(quote.getOrganizationId())
                .productPlanId(quote.getProductPlanId())
                .productName(product != null ? product.getName() : null)
                .planName(plan != null ? plan.getName() : null)
                .premiumAmount(quote.getPremiumAmount())
                .premiumFrequency(quote.getPremiumFrequency())
                .validUntil(quote.getValidUntil())
                .status(quote.getStatus())
                .answers(answers != null ? answers : loadQuoteAnswers(quote.getId()))
                .createdAt(quote.getCreatedAt())
                .build();
    }

    private ApplicationResponse toApplicationResponse(PolicyApplication application, Map<String, String> answers) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .applicationReference(application.getApplicationReference())
                .quoteId(application.getQuoteId())
                .citizenProfileId(application.getCitizenProfileId())
                .organizationId(application.getOrganizationId())
                .productPlanId(application.getProductPlanId())
                .consentId(application.getConsentId())
                .premiumAmount(application.getPremiumAmount())
                .premiumFrequency(application.getPremiumFrequency())
                .status(application.getStatus())
                .decisionReason(application.getDecisionReason())
                .reviewedBy(application.getReviewedBy())
                .reviewedAt(application.getReviewedAt())
                .submittedAt(application.getSubmittedAt())
                .answers(answers)
                .build();
    }
}
