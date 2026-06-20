package com.ingoboka_api.v1.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ingoboka_api.v1.identity.services.OtpService;
import com.ingoboka_api.v1.support.IntegrationTestSupport;
import com.ingoboka_api.v1.support.OtpTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

@EnabledIf("com.ingoboka_api.v1.support.IntegrationTestSupport#isEnabled")
class CitizenEndpointsIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private OtpService otpService;

    private String citizenToken;

    @BeforeEach
    void registerAndVerifyCitizen() throws Exception {
        String phone = "+25079" + randomPhoneSuffix();
        String email = "citizen-flow." + randomPhoneSuffix() + "@test.ingoboka";
        String nationalId = "119977665544" + randomPhoneSuffix().substring(0, 4);
        post(
                        "/api/v1/auth/register",
                        """
                        {"fullName":"Flow Citizen","phone":"%s","nationalId":"%s","password":"Citizen@Test123","email":"%s"}
                        """
                                .formatted(phone, nationalId, email))
                .andExpect(status().isCreated());

        String otp = OtpTestSupport.latestOtpFor(otpService, "SIGNUP", phone);
        String verifyResponse = post(
                        "/api/v1/auth/verify-otp",
                        """
                        {"phoneNumber":"%s","otp":"%s"}
                        """
                                .formatted(phone, otp))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        citizenToken = objectMapper.readTree(verifyResponse).path("data").path("accessToken").asText();
    }

    @Test
    void citizenProfileAndNotificationEndpointsRespond() throws Exception {
        assertNotServerError(get("/api/v1/customers/me/profile", citizenToken));
        assertNotServerError(get("/api/v1/customers/me/dependants", citizenToken));
        assertNotServerError(get("/api/v1/customers/me/consents", citizenToken));
        assertNotServerError(get("/api/v1/notifications/me", citizenToken));
        assertNotServerError(get("/api/v1/policies/me", citizenToken));
        assertNotServerError(get("/api/v1/payments/me", citizenToken));
        assertNotServerError(get("/api/v1/applications/me", citizenToken));
        assertNotServerError(get("/api/v1/claims/me", citizenToken));
    }

    @Test
    void citizenWriteValidationReturnsClientErrors() throws Exception {
        assertNotServerError(post("/api/v1/customers/me/dependants", "{}", citizenToken));
        assertNotServerError(post("/api/v1/applications/quote", "{}", citizenToken));
        assertNotServerError(post("/api/v1/claims", "{}", citizenToken));
    }

    @Test
    void citizenCannotAccessAgentApplications() throws Exception {
        get("/api/v1/agent/applications", citizenToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
}
