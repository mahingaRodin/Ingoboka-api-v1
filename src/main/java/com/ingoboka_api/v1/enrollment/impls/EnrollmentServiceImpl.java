package com.ingoboka_api.v1.enrollment.impls;

import com.ingoboka_api.v1.common.enums.ApplicationStatus;
import com.ingoboka_api.v1.common.enums.QuoteStatus;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.requests.GenerateQuoteRequest;
import com.ingoboka_api.v1.common.requests.ReviewApplicationRequest;
import com.ingoboka_api.v1.common.requests.SubmitApplicationRequest;
import com.ingoboka_api.v1.common.responses.ApplicationResponse;
import com.ingoboka_api.v1.common.responses.QuoteResponse;
import com.ingoboka_api.v1.common.security.IngobokaUserDetails;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.customer.models.CitizenProfile;
import com.ingoboka_api.v1.customer.models.Consent;
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
import com.ingoboka_api.v1.product.models.EligibilityRule;
import com.ingoboka_api.v1.product.models.InsuranceProduct;
import com.ingoboka_api.v1.product.models.ProductPlan;
import com.ingoboka_api.v1.product.repositories.EligibilityRuleRepository;
import com.ingoboka_api.v1.product.repositories.InsuranceProductRepository;
import com.ingoboka_api.v1.product.services.ProductCatalogService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
    private final EligibilityRuleRepository eligibilityRuleRepository;

    @Override
    @Transactional
    public QuoteResponse generateQuote(GenerateQuoteRequest request) {
        IngobokaUserDetails user = SecurityUtils.currentUser();
        CitizenProfile profile = customerProfileService.requireProfileForUser(user.getUserId());
        if (profile.getDateOfBirth() == null) {
            throw new BusinessException("Complete your profile with date of birth before requesting a quote");
        }

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

        saveAnswers(request.getAnswers(), quote.getId(), now);
        return toQuoteResponse(quote, request.getAnswers());
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

        return toApplicationResponse(application, loadAnswers(application.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> listMyApplications() {
        CitizenProfile profile =
                customerProfileService.requireProfileForUser(SecurityUtils.currentUser().getUserId());
        return applicationRepository.findByCitizenProfileIdOrderBySubmittedAtDesc(profile.getId()).stream()
                .map(app -> toApplicationResponse(app, loadAnswers(app.getId())))
                .toList();
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
    public List<ApplicationResponse> listTenantApplications(ApplicationStatus status) {
        UUID orgId = requireUnderwriterOrganizationId();
        List<PolicyApplication> applications = status == null
                ? applicationRepository.findByOrganizationIdOrderBySubmittedAtDesc(orgId)
                : applicationRepository.findByOrganizationIdAndStatusOrderBySubmittedAtDesc(orgId, status);
        return applications.stream()
                .map(app -> toApplicationResponse(app, loadAnswers(app.getId())))
                .toList();
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
        return toApplicationResponse(application, loadAnswers(application.getId()));
    }

    private void validateEligibility(CitizenProfile profile, UUID planId) {
        int age = Period.between(profile.getDateOfBirth(), LocalDate.now()).getYears();
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
        return QuoteResponse.builder()
                .id(quote.getId())
                .quoteReference(quote.getQuoteReference())
                .citizenProfileId(quote.getCitizenProfileId())
                .organizationId(quote.getOrganizationId())
                .productPlanId(quote.getProductPlanId())
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
