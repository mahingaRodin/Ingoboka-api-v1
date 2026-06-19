package com.ingoboka_api.v1.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ingoboka_api.v1.identity.services.OtpService;
import com.ingoboka_api.v1.support.IntegrationTestSupport;
import com.ingoboka_api.v1.support.OtpTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;

@EnabledIf("com.ingoboka_api.v1.support.IntegrationTestSupport#isEnabled")
class AuthControllerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private OtpService otpService;

    @Test
    void otpDeliveryConfigIsPublic() throws Exception {
        get("/api/v1/auth/otp-delivery-config")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deliveryChannel").exists());
    }

    @Test
    void platformAdminCanLogin() throws Exception {
        String token = loginPlatformAdmin();
        get("/api/v1/admin/dashboard", token).andExpect(status().isOk());
    }

    @Test
    void citizenRegistrationVerifyOtpAndLogin() throws Exception {
        String phone = "+25078" + randomPhoneSuffix();
        String email = "citizen." + randomPhoneSuffix() + "@test.ingoboka";
        String nationalId = "119988776655" + randomPhoneSuffix().substring(0, 4);
        String registerBody =
                """
                {"fullName":"Test Citizen","phone":"%s","nationalId":"%s","password":"Citizen@Test123","email":"%s"}
                """
                        .formatted(phone, nationalId, email);

        post("/api/v1/auth/register", registerBody).andExpect(status().isCreated());

        String otp = OtpTestSupport.latestOtpFor(otpService, "SIGNUP", phone);
        String verifyBody =
                """
                {"phone":"%s","otp":"%s"}
                """
                        .formatted(phone, otp);

        post("/api/v1/auth/verify-otp", verifyBody)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists());

        post("/api/v1/auth/login", """
                {"identifier":"%s","password":"Citizen@Test123"}
                """.formatted(phone))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        post("/api/v1/auth/login", """
                {"identifier":"%s","password":"wrong-password"}
                """.formatted(platformAdminEmail()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerValidationFailsForMissingFields() throws Exception {
        post("/api/v1/auth/register", "{}").andExpect(status().isBadRequest());
    }

    @Test
    void refreshAndLogoutAcceptRequests() throws Exception {
        String loginResponse = post(
                        "/api/v1/auth/login",
                        """
                        {"identifier":"%s","password":"%s"}
                        """
                                .formatted(platformAdminEmail(), platformAdminPassword()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = objectMapper.readTree(loginResponse).path("data").path("refreshToken").asText();

        post("/api/v1/auth/refresh", """
                {"refreshToken":"%s"}
                """.formatted(refreshToken))
                .andExpect(status().isOk());

        post("/api/v1/auth/logout", """
                {"refreshToken":"%s"}
                """.formatted(refreshToken))
                .andExpect(status().isOk());
    }
}
