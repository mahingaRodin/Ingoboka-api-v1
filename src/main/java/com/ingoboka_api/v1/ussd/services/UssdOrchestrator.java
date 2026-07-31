package com.ingoboka_api.v1.ussd.services;

import com.ingoboka_api.v1.billing.services.BillingService;
import com.ingoboka_api.v1.common.enums.ProductStatus;
import com.ingoboka_api.v1.common.enums.UssdRegistrationType;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.responses.ApplicationResponse;
import com.ingoboka_api.v1.common.responses.PaymentResponse;
import com.ingoboka_api.v1.common.util.PhoneNumberUtils;
import com.ingoboka_api.v1.customer.models.CitizenProfile;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.enrollment.services.EnrollmentService;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.integration.payment.SandboxPaymentAdapter;
import com.ingoboka_api.v1.policy.models.Policy;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import com.ingoboka_api.v1.product.models.InsuranceProduct;
import com.ingoboka_api.v1.product.models.ProductPlan;
import com.ingoboka_api.v1.product.repositories.InsuranceProductRepository;
import com.ingoboka_api.v1.product.repositories.ProductPlanRepository;
import com.ingoboka_api.v1.ussd.menu.UssdMessages;
import com.ingoboka_api.v1.ussd.models.UssdRegistration;
import com.ingoboka_api.v1.ussd.session.UssdSession;
import com.ingoboka_api.v1.ussd.session.UssdSessionStore;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class UssdOrchestrator {

    private final UssdSessionStore sessionStore;
    private final UssdRegistrationService registrationService;
    private final InsuranceProductRepository productRepository;
    private final ProductPlanRepository planRepository;
    private final UserRepository userRepository;
    private final CitizenProfileRepository citizenProfileRepository;
    private final PolicyRepository policyRepository;
    private final EnrollmentService enrollmentService;
    private final BillingService billingService;

    public String handle(String sessionId, String phoneNumber, String serviceCode, String text) {
        String phone = PhoneNumberUtils.normalizeRwanda(phoneNumber);
        UssdSession session = sessionStore
                .find(sessionId)
                .orElseGet(() -> newSession(sessionId, phone, serviceCode));

        String[] parts = parseText(text);
        String input = parts.length == 0 ? "" : parts[parts.length - 1];

        if (!StringUtils.hasText(text)) {
            session.setStep("MAIN");
            String response = UssdMessages.mainMenu(session.getLanguage());
            sessionStore.save(session);
            return response;
        }

        if ("0".equals(input) && !"MAIN".equals(session.getStep()) && !"LANGUAGE".equals(session.getStep())) {
            session.setStep("MAIN");
            sessionStore.save(session);
            return UssdMessages.mainMenu(session.getLanguage());
        }

        String response = switch (session.getStep()) {
            case "MAIN" -> handleMain(session, input);
            case "LANGUAGE" -> handleLanguage(session, input);
            case "REGISTER_TYPE" -> handleRegisterType(session, input);
            case "REGISTER_NAME" -> handleRegisterName(session, input);
            case "REGISTER_BUSINESS" -> handleRegisterBusiness(session, input);
            case "REGISTER_DISTRICT" -> handleRegisterDistrict(session, input);
            case "SERVICES" -> handleServices(session, input);
            case "PLANS" -> handlePlans(session, input);
            case "POLICIES" -> handlePolicies(session, input);
            case "PAY" -> handlePay(session, input);
            default -> {
                session.setStep("MAIN");
                yield UssdMessages.mainMenu(session.getLanguage());
            }
        };

        if (response.startsWith("END")) {
            sessionStore.delete(sessionId);
        } else {
            sessionStore.save(session);
        }
        return response;
    }

    private String lang(UssdSession session) {
        return session.getLanguage();
    }

    private String handleMain(UssdSession session, String input) {
        return switch (input) {
            case "1" -> {
                session.setStep("SERVICES");
                yield listServices(session);
            }
            case "2" -> {
                session.setStep("POLICIES");
                yield listPolicies(session);
            }
            case "3" -> {
                session.setStep("PAY");
                yield listPayOptions(session);
            }
            case "4" -> startRegistration(session);
            case "5" -> UssdMessages.help(lang(session));
            case "6" -> {
                session.setStep("LANGUAGE");
                yield UssdMessages.languageMenu();
            }
            default -> UssdMessages.invalidOption(lang(session));
        };
    }

    private String handleLanguage(UssdSession session, String input) {
        if ("1".equals(input)) {
            session.setLanguage("rw");
            session.setStep("MAIN");
            return UssdMessages.mainMenu("rw");
        }
        if ("2".equals(input)) {
            session.setLanguage("en");
            session.setStep("MAIN");
            return UssdMessages.mainMenu("en");
        }
        return UssdMessages.languageMenu();
    }

    private String startRegistration(UssdSession session) {
        Optional<UssdRegistration> existing = registrationService.findByPhone(session.getPhoneNumber());
        if (existing.isPresent()) {
            UssdRegistration reg = existing.get();
            return UssdMessages.alreadyRegistered(lang(session), reg.getReferenceCode(), reg.getFullName());
        }
        session.setStep("REGISTER_TYPE");
        return UssdMessages.registrationTypePrompt(lang(session));
    }

    private String handleRegisterType(UssdSession session, String input) {
        if ("1".equals(input)) {
            session.put("regType", UssdRegistrationType.FAMILY.name());
            session.setStep("REGISTER_NAME");
            return UssdMessages.askFullName(lang(session));
        }
        if ("2".equals(input)) {
            session.put("regType", UssdRegistrationType.BUSINESS.name());
            session.setStep("REGISTER_NAME");
            return UssdMessages.askFullName(lang(session));
        }
        return UssdMessages.invalidOption(lang(session));
    }

    private String handleRegisterName(UssdSession session, String input) {
        if (!StringUtils.hasText(input) || input.length() < 2) {
            return UssdMessages.askFullName(lang(session));
        }
        session.put("fullName", input.trim());
        if (UssdRegistrationType.BUSINESS.name().equals(session.get("regType"))) {
            session.setStep("REGISTER_BUSINESS");
            return UssdMessages.askBusinessName(lang(session));
        }
        session.setStep("REGISTER_DISTRICT");
        return UssdMessages.askDistrict(lang(session));
    }

    private String handleRegisterBusiness(UssdSession session, String input) {
        if (!StringUtils.hasText(input) || input.length() < 2) {
            return UssdMessages.askBusinessName(lang(session));
        }
        session.put("businessName", input.trim());
        session.setStep("REGISTER_DISTRICT");
        return UssdMessages.askDistrict(lang(session));
    }

    private String handleRegisterDistrict(UssdSession session, String input) {
        if (!StringUtils.hasText(input) || input.length() < 2) {
            return UssdMessages.askDistrict(lang(session));
        }
        try {
            UssdRegistrationType type = UssdRegistrationType.valueOf(session.get("regType"));
            UssdRegistration reg = registrationService.register(
                    session.getPhoneNumber(),
                    type,
                    session.get("fullName"),
                    session.get("businessName"),
                    input.trim(),
                    session.getLanguage());
            String display = type == UssdRegistrationType.BUSINESS && session.get("businessName") != null
                    ? session.get("businessName")
                    : session.get("fullName");
            return UssdMessages.registrationSuccess(
                    lang(session), reg.getReferenceCode(), display, type.name());
        } catch (BusinessException ex) {
            return "END " + ex.getMessage();
        }
    }

    private String listServices(UssdSession session) {
        List<InsuranceProduct> products =
                productRepository.findByStatusOrderByPublishedAtDesc(ProductStatus.PUBLISHED);
        if (products.isEmpty()) {
            return UssdMessages.noServices(lang(session));
        }
        StringBuilder sb = new StringBuilder(UssdMessages.servicesHeader(lang(session)));
        int limit = Math.min(products.size(), 5);
        for (int i = 0; i < limit; i++) {
            InsuranceProduct p = products.get(i);
            session.put("product:" + (i + 1), p.getId().toString());
            sb.append(i + 1).append(". ").append(shorten(p.getName(), 28)).append("\n");
        }
        sb.append(UssdMessages.backLabel(lang(session)));
        return sb.toString();
    }

    private String handleServices(UssdSession session, String input) {
        String productId = session.get("product:" + input);
        if (productId == null) {
            return UssdMessages.invalidOption(lang(session));
        }
        session.put("productId", productId);
        session.setStep("PLANS");
        return listPlans(session, UUID.fromString(productId));
    }

    private String listPlans(UssdSession session, UUID productId) {
        List<ProductPlan> plans = planRepository.findByProductIdAndStatus(productId, ProductStatus.PUBLISHED);
        if (plans.isEmpty()) {
            return UssdMessages.noPlans(lang(session));
        }
        StringBuilder sb = new StringBuilder(UssdMessages.plansHeader(lang(session)));
        int limit = Math.min(plans.size(), 5);
        for (int i = 0; i < limit; i++) {
            ProductPlan plan = plans.get(i);
            session.put("plan:" + (i + 1), plan.getId().toString());
            sb.append(i + 1)
                    .append(". ")
                    .append(shorten(plan.getName(), 18))
                    .append(" ")
                    .append(plan.getPremiumAmount().toPlainString())
                    .append("RWF/")
                    .append(plan.getPremiumFrequency().name().charAt(0))
                    .append("\n");
        }
        sb.append(UssdMessages.backLabel(lang(session)));
        return sb.toString();
    }

    private String handlePlans(UssdSession session, String input) {
        String planId = session.get("plan:" + input);
        if (planId == null) {
            return UssdMessages.invalidOption(lang(session));
        }
        if (!registrationService.isRegistered(session.getPhoneNumber())
                && userRepository.findByPhoneNumber(session.getPhoneNumber()).isEmpty()) {
            return UssdMessages.mustRegister(lang(session));
        }
        try {
            ApplicationResponse app = enrollmentService.createAgentAssistedApplication(
                    session.getPhoneNumber(), UUID.fromString(planId));
            String productName = "cover";
            String productId = session.get("productId");
            if (productId != null) {
                productName = productRepository
                        .findById(UUID.fromString(productId))
                        .map(InsuranceProduct::getName)
                        .orElse(productName);
            }
            return UssdMessages.enrollSuccess(
                    lang(session),
                    shorten(productName, 24),
                    app.getApplicationReference() != null
                            ? app.getApplicationReference()
                            : app.getId().toString());
        } catch (BusinessException ex) {
            return "END " + shorten(ex.getMessage(), 120);
        }
    }

    private String listPolicies(UssdSession session) {
        Optional<CitizenProfile> profile = findProfile(session.getPhoneNumber());
        if (profile.isEmpty()) {
            return UssdMessages.mustRegister(lang(session));
        }
        List<Policy> policies = policyRepository.findByCitizenProfileIdOrderByCreatedAtDesc(profile.get().getId());
        if (policies.isEmpty()) {
            return UssdMessages.noPolicies(lang(session));
        }
        StringBuilder sb = new StringBuilder(UssdMessages.policiesHeader(lang(session)));
        int limit = Math.min(policies.size(), 4);
        for (int i = 0; i < limit; i++) {
            Policy p = policies.get(i);
            sb.append(i + 1)
                    .append(". ")
                    .append(p.getPolicyNumber())
                    .append(" ")
                    .append(p.getStatus().name())
                    .append("\n");
        }
        return sb.toString().trim();
    }

    private String handlePolicies(UssdSession session, String input) {
        return listPolicies(session);
    }

    private String listPayOptions(UssdSession session) {
        Optional<CitizenProfile> profile = findProfile(session.getPhoneNumber());
        if (profile.isEmpty()) {
            return UssdMessages.mustRegister(lang(session));
        }
        List<Policy> policies = policyRepository.findByCitizenProfileIdOrderByCreatedAtDesc(profile.get().getId());
        if (policies.isEmpty()) {
            return UssdMessages.noPremiumDue(lang(session));
        }
        StringBuilder sb = new StringBuilder(UssdMessages.payHeader(lang(session)));
        int limit = Math.min(policies.size(), 5);
        for (int i = 0; i < limit; i++) {
            Policy p = policies.get(i);
            session.put("pay:" + (i + 1), p.getId().toString());
            sb.append(i + 1)
                    .append(". ")
                    .append(p.getPolicyNumber())
                    .append(" ")
                    .append(p.getPremiumAmount().toPlainString())
                    .append("RWF\n");
        }
        sb.append(UssdMessages.backLabel(lang(session)));
        return sb.toString();
    }

    private String handlePay(UssdSession session, String input) {
        String policyId = session.get("pay:" + input);
        if (policyId == null) {
            return UssdMessages.invalidOption(lang(session));
        }
        Optional<CitizenProfile> profile = findProfile(session.getPhoneNumber());
        if (profile.isEmpty()) {
            return UssdMessages.mustRegister(lang(session));
        }
        try {
            PaymentResponse payment = billingService.initiatePaymentForCitizen(
                    profile.get().getId(),
                    UUID.fromString(policyId),
                    SandboxPaymentAdapter.CODE,
                    session.getPhoneNumber());
            return UssdMessages.paymentInitiated(
                    lang(session),
                    payment.getAmount() != null ? payment.getAmount().toPlainString() : "0",
                    payment.getProviderReference() != null
                            ? payment.getProviderReference()
                            : payment.getId().toString());
        } catch (BusinessException ex) {
            return "END " + shorten(ex.getMessage(), 120);
        }
    }

    private Optional<CitizenProfile> findProfile(String phone) {
        return userRepository
                .findByPhoneNumber(PhoneNumberUtils.normalizeRwanda(phone))
                .flatMap(user -> citizenProfileRepository.findByUserId(user.getId()));
    }

    private UssdSession newSession(String sessionId, String phone, String serviceCode) {
        UssdSession session = new UssdSession();
        session.setSessionId(sessionId);
        session.setPhoneNumber(phone);
        session.setServiceCode(serviceCode);
        session.setStep("MAIN");
        session.setLanguage("rw");
        return session;
    }

    private static String[] parseText(String text) {
        if (!StringUtils.hasText(text)) {
            return new String[0];
        }
        return Arrays.stream(text.split("\\*")).map(String::trim).filter(StringUtils::hasText).toArray(String[]::new);
    }

    private static String shorten(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }
}
