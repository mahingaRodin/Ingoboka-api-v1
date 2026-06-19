package com.ingoboka_api.v1.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ingoboka_api.v1.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

@EnabledIf("com.ingoboka_api.v1.support.IntegrationTestSupport#isEnabled")
class WriteOperationsIntegrationTest extends IntegrationTestSupport {

    private String adminToken;
    private final UUID missingId = UUID.fromString("00000000-0000-4000-8000-000000000099");

    @BeforeEach
    void login() throws Exception {
        adminToken = loginPlatformAdmin();
    }

    @Test
    void invalidWritePayloadsReturnClientErrorsNotServerErrors() throws Exception {
        assertNotServerError(post("/api/v1/partners", "{}", adminToken));
        assertNotServerError(post("/api/v1/products", "{}", adminToken));
        assertNotServerError(post("/api/v1/applications", "{}", adminToken));
        assertNotServerError(post("/api/v1/claims", "{}", adminToken));
        assertNotServerError(post("/api/v1/payments", "{}", adminToken));
        assertNotServerError(post("/api/v1/admin/users", "{}", adminToken));
        assertNotServerError(post("/api/v1/documents", "{}", adminToken));
        assertNotServerError(post("/api/v1/revenue/price-rules", "{}", adminToken));
        assertNotServerError(post("/api/v1/billing/refunds", "{}", adminToken));
        assertNotServerError(post("/api/v1/partners/" + missingId + "/staff", "{}", adminToken));
        assertNotServerError(post("/api/v1/partners/" + missingId + "/contracts", "{}", adminToken));
    }

    @Test
    void publicAuthWriteEndpointsValidateInput() throws Exception {
        post("/api/v1/auth/register", "{}").andExpect(status().isBadRequest());
        post("/api/v1/auth/signup", "{}").andExpect(status().isBadRequest());
        post("/api/v1/auth/login", "{}").andExpect(status().isBadRequest());
        post("/api/v1/auth/verify-otp", "{}").andExpect(status().isBadRequest());
        post("/api/v1/auth/forgot-password/request", "{}").andExpect(status().isBadRequest());
    }

    @Test
    void webhookAndVerifyEndpointsAreReachable() throws Exception {
        assertNotServerError(get("/api/v1/policies/verify/" + missingId));
        assertNotServerError(get("/api/v1/verify/" + missingId));
        assertNotServerError(post("/api/v1/payments/webhooks/momo", "{}"));
        assertNotServerError(post("/api/v1/payments/webhooks/test-provider", "{}"));
    }
}
