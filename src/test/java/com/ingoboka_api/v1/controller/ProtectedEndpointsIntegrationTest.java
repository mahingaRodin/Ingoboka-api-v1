package com.ingoboka_api.v1.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ingoboka_api.v1.support.IntegrationTestSupport;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

@EnabledIf("com.ingoboka_api.v1.support.IntegrationTestSupport#isEnabled")
class ProtectedEndpointsIntegrationTest extends IntegrationTestSupport {

    private String adminToken;
    private final UUID missingId = UUID.fromString("00000000-0000-4000-8000-000000000099");

    @BeforeEach
    void login() throws Exception {
        adminToken = loginPlatformAdmin();
    }

    @Test
    void protectedEndpointsRejectMissingAuth() throws Exception {
        get("/api/v1/admin/dashboard").andExpect(status().isUnauthorized());
        get("/api/v1/partners").andExpect(status().isUnauthorized());
        get("/api/v1/products").andExpect(status().isUnauthorized());
        get("/api/v1/claims").andExpect(status().isUnauthorized());
    }

    @Test
    void platformAdminDashboardEndpointsRespond() throws Exception {
        assertNotServerError(get("/api/v1/admin/dashboard", adminToken));
        assertNotServerError(get("/api/v1/admin/dashboard/tenant-overview", adminToken));
    }

    @Test
    void platformAdminUserEndpointsRespond() throws Exception {
        assertNotServerError(get("/api/v1/admin/users", adminToken));
        assertNotServerError(get("/api/v1/admin/users/" + missingId, adminToken));
    }

    @Test
    void partnerManagementEndpointsRespond() throws Exception {
        assertNotServerError(get("/api/v1/partners", adminToken));
        assertNotServerError(get("/api/v1/partners/" + missingId, adminToken));
        assertNotServerError(get("/api/v1/partners/" + missingId + "/staff", adminToken));
        assertNotServerError(get("/api/v1/partners/" + missingId + "/contracts", adminToken));
    }

    @Test
    void productCatalogEndpointsRespond() throws Exception {
        assertNotServerError(get("/api/v1/products", adminToken));
        assertNotServerError(get("/api/v1/products/tenant", adminToken));
        assertNotServerError(get("/api/v1/products/" + missingId, adminToken));
        assertNotServerError(get("/api/v1/products/" + missingId + "/plans", adminToken));
    }

    @Test
    void enrollmentAndPolicyEndpointsRespond() throws Exception {
        assertNotServerError(get("/api/v1/applications", adminToken));
        assertNotServerError(get("/api/v1/applications/" + missingId, adminToken));
        assertNotServerError(get("/api/v1/policies/tenant", adminToken));
        assertNotServerError(get("/api/v1/policies/" + missingId, adminToken));
    }

    @Test
    void claimsAndBillingEndpointsRespond() throws Exception {
        assertNotServerError(get("/api/v1/claims", adminToken));
        assertNotServerError(get("/api/v1/claims/" + missingId, adminToken));
        assertNotServerError(get("/api/v1/payments/me", adminToken));
        assertNotServerError(get("/api/v1/billing/reconciliation", adminToken));
        assertNotServerError(get("/api/v1/billing/bills/policy/" + missingId, adminToken));
    }

    @Test
    void reportingRevenueAuditEndpointsRespond() throws Exception {
        assertNotServerError(get("/api/v1/reports/overview", adminToken));
        assertNotServerError(get("/api/v1/reports/policies", adminToken));
        assertNotServerError(get("/api/v1/reports/claims", adminToken));
        assertNotServerError(get("/api/v1/reports/payments", adminToken));
        assertNotServerError(get("/api/v1/revenue/price-rules", adminToken));
        assertNotServerError(get("/api/v1/revenue/ledger", adminToken));
        assertNotServerError(get("/api/v1/revenue/invoices", adminToken));
        assertNotServerError(get("/api/v1/audit/logs", adminToken));
        assertNotServerError(get("/api/v1/audit/data-subject-requests", adminToken));
    }

    @Test
    void integrationDocumentNotificationEndpointsRespond() throws Exception {
        assertNotServerError(get("/api/v1/integrations", adminToken));
        assertNotServerError(get("/api/v1/documents", adminToken));
        assertNotServerError(get("/api/v1/documents/" + missingId, adminToken));
        assertNotServerError(get("/api/v1/notifications/me", adminToken));
        assertNotServerError(get("/api/v1/agent/dashboard", adminToken));
        assertNotServerError(get("/api/v1/insurer/settings", adminToken));
    }

    @Test
    void frontendCompatAdminEndpointsRespond() throws Exception {
        assertNotServerError(get("/api/v1/admin/claims", adminToken));
        assertNotServerError(get("/api/v1/admin/reports/overview", adminToken));
        assertNotServerError(get("/api/v1/admin/platform/overview", adminToken));
    }

    @Test
    void publicActuatorAndDocsEndpointsRespond() throws Exception {
        get("/actuator/health").andExpect(status().isOk());
        get("/actuator/info").andExpect(status().isOk());
        assertNotServerError(get("/api-docs"));
        assertNotServerError(get("/swagger-ui/index.html"));
    }
}
