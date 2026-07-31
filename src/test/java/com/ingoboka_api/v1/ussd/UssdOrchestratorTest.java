package com.ingoboka_api.v1.ussd;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ingoboka_api.v1.billing.services.BillingService;
import com.ingoboka_api.v1.common.enums.UssdRegistrationType;
import com.ingoboka_api.v1.customer.repositories.CitizenProfileRepository;
import com.ingoboka_api.v1.enrollment.services.EnrollmentService;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.policy.repositories.PolicyRepository;
import com.ingoboka_api.v1.product.repositories.InsuranceProductRepository;
import com.ingoboka_api.v1.product.repositories.ProductPlanRepository;
import com.ingoboka_api.v1.ussd.menu.UssdMessages;
import com.ingoboka_api.v1.ussd.models.UssdRegistration;
import com.ingoboka_api.v1.ussd.services.UssdOrchestrator;
import com.ingoboka_api.v1.ussd.services.UssdRegistrationService;
import com.ingoboka_api.v1.ussd.session.InMemoryUssdSessionStore;
import com.ingoboka_api.v1.ussd.session.UssdSessionStore;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UssdOrchestratorTest {

    @Mock
    private UssdRegistrationService registrationService;

    @Mock
    private InsuranceProductRepository productRepository;

    @Mock
    private ProductPlanRepository planRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CitizenProfileRepository citizenProfileRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private EnrollmentService enrollmentService;

    @Mock
    private BillingService billingService;

    private UssdSessionStore sessionStore;
    private UssdOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        sessionStore = new InMemoryUssdSessionStore();
        orchestrator = new UssdOrchestrator(
                sessionStore,
                registrationService,
                productRepository,
                planRepository,
                userRepository,
                citizenProfileRepository,
                policyRepository,
                enrollmentService,
                billingService);
    }

    @Test
    void emptyTextReturnsMainMenuWithoutHeader() {
        String response = orchestrator.handle("s1", "+250780000099", "*477#", "");
        assertThat(response).startsWith("CON");
        assertThat(response).doesNotContain("Ingoboka");
        assertThat(response).doesNotContain("*477#");
        assertThat(response).contains("Serivise");
        assertThat(response).contains("Kwiyandikisha");
        assertThat(response).contains("Hitamo ururimi");
    }

    @Test
    void chooseLanguageSwitchesToEnglish() {
        orchestrator.handle("s6", "+250780000099", "*477#", "");
        orchestrator.handle("s6", "+250780000099", "*477#", "6");
        String response = orchestrator.handle("s6", "+250780000099", "*477#", "6*2");
        assertThat(response).contains("Insurance services");
        assertThat(response).contains("Choose language");
    }

    @Test
    void helpEndsSession() {
        String response = orchestrator.handle("s2", "+250780000099", "*477#", "5");
        assertThat(response).startsWith("END");
        assertThat(response).contains("DEMO/SANDBOX");
    }

    @Test
    void duplicateRegistrationIsBlocked() {
        UssdRegistration existing = new UssdRegistration();
        existing.setReferenceCode("USSD123456");
        existing.setFullName("Aline Uwase");
        when(registrationService.findByPhone("+250780000099")).thenReturn(Optional.of(existing));

        String response = orchestrator.handle("s3", "+250780000099", "*477#", "4");
        assertThat(response).startsWith("END");
        assertThat(response).contains("Mwamaze kwiyandikisha");
        assertThat(response).contains("USSD123456");
    }

    @Test
    void familyRegistrationCompletesAndSendsThroughService() {
        when(registrationService.findByPhone("+250780000088")).thenReturn(Optional.empty());
        UssdRegistration saved = new UssdRegistration();
        saved.setReferenceCode("USSD654321");
        saved.setFullName("Aline Uwase");
        when(registrationService.register(
                        eq("+250780000088"),
                        eq(UssdRegistrationType.FAMILY),
                        eq("Aline Uwase"),
                        any(),
                        eq("Gasabo"),
                        any()))
                .thenReturn(saved);

        orchestrator.handle("s4", "+250780000088", "*477#", "");
        orchestrator.handle("s4", "+250780000088", "*477#", "4");
        orchestrator.handle("s4", "+250780000088", "*477#", "4*1");
        orchestrator.handle("s4", "+250780000088", "*477#", "4*1*Aline Uwase");
        String response = orchestrator.handle("s4", "+250780000088", "*477#", "4*1*Aline Uwase*Gasabo");

        assertThat(response).startsWith("END");
        assertThat(response).contains("USSD654321");
        verify(registrationService)
                .register(
                        eq("+250780000088"),
                        eq(UssdRegistrationType.FAMILY),
                        eq("Aline Uwase"),
                        any(),
                        eq("Gasabo"),
                        any());
    }

    @Test
    void mainMenuMessageIsShortEnoughForUssd() {
        assertThat(UssdMessages.mainMenu("rw").length()).isLessThan(180);
        assertThat(UssdMessages.mainMenu("en").length()).isLessThan(180);
    }

    @Test
    void noProductsReturnsFriendlyEnd() {
        when(productRepository.findByStatusOrderByPublishedAtDesc(any())).thenReturn(List.of());
        orchestrator.handle("s5", "+250780000077", "*477#", "");
        String response = orchestrator.handle("s5", "+250780000077", "*477#", "1");
        assertThat(response).contains("Nta serivisi");
    }
}
