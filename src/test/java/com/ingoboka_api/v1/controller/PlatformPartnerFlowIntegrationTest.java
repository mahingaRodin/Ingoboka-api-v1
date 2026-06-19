package com.ingoboka_api.v1.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.ingoboka_api.v1.support.IntegrationTestSupport;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIf;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIf("com.ingoboka_api.v1.support.IntegrationTestSupport#isEnabled")
class PlatformPartnerFlowIntegrationTest extends IntegrationTestSupport {

    private static String adminToken;
    private static String partnerId;
    private static String partnerAdminToken;

    @Test
    @Order(1)
    void platformAdminLogsIn() throws Exception {
        adminToken = loginPlatformAdmin();
    }

    @Test
    @Order(2)
    void onboardPartnerOrganization() throws Exception {
        String suffix = randomPhoneSuffix();
        String body =
                """
                {
                  "name":"Test Insurer %s",
                  "code":"TEST_INS_%s",
                  "type":"INSURER",
                  "registrationNumber":"REG-%s",
                  "contactEmail":"contact-%s@test.ingoboka",
                  "contactPhone":"+250788%s",
                  "adminFirstName":"Eric",
                  "adminLastName":"Test",
                  "adminEmail":"partner-admin-%s@test.ingoboka",
                  "adminPhone":"+250789%s",
                  "adminDefaultPassword":"Partner@Test123"
                }
                """
                        .formatted(suffix, suffix, suffix, suffix, suffix.substring(0, 6), suffix, suffix.substring(0, 6));

        String response = post("/api/v1/partners", body, adminToken)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.partner.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode root = objectMapper.readTree(response);
        partnerId = root.path("data").path("partner").path("id").asText();
    }

    @Test
    @Order(3)
    void listAndFetchPartner() throws Exception {
        get("/api/v1/partners", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());

        get("/api/v1/partners/" + partnerId, adminToken).andExpect(status().isOk());
    }

    @Test
    @Order(4)
    void partnerAdminFirstLoginRequiresPasswordChange() throws Exception {
        String adminEmail = findPartnerAdminEmail();
        String partnerLoginResponse = post(
                        "/api/v1/auth/login",
                        """
                        {"identifier":"%s","password":"Partner@Test123"}
                        """
                                .formatted(adminEmail))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.mustChangePassword").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        partnerAdminToken = objectMapper.readTree(partnerLoginResponse).path("data").path("accessToken").asText();
    }

    @Test
    @Order(5)
    void partnerPortalStaffOverviewRespondsAfterLogin() throws Exception {
        assertNotServerError(get("/api/v1/partner/staff/overview", partnerAdminToken));
        assertNotServerError(get("/api/v1/partner/staff", partnerAdminToken));
    }

    @Test
    @Order(6)
    void createPartnerContractAndStaff() throws Exception {
        String contractBody =
                """
                {
                  "contractReference":"CON-%s",
                  "startDate":"2026-01-01",
                  "endDate":"2027-01-01",
                  "notes":"Integration test contract"
                }
                """
                        .formatted(randomPhoneSuffix());

        post("/api/v1/partners/" + partnerId + "/contracts", contractBody, adminToken)
                .andExpect(status().isCreated());

        get("/api/v1/partners/" + partnerId + "/contracts", adminToken).andExpect(status().isOk());

        String staffBody =
                """
                {
                  "firstName":"Claims",
                  "lastName":"Officer",
                  "email":"claims-%s@test.ingoboka",
                  "phoneNumber":"+250787%s",
                  "roleCode":"CLAIMS_OFFICER"
                }
                """
                        .formatted(randomPhoneSuffix(), randomPhoneSuffix().substring(0, 6));

        post("/api/v1/partners/" + partnerId + "/staff", staffBody, adminToken).andExpect(status().isCreated());
    }

    private String findPartnerAdminEmail() throws Exception {
        String staffResponse = get("/api/v1/partners/" + partnerId + "/staff", adminToken)
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode content = objectMapper.readTree(staffResponse).path("data").path("content");
        for (JsonNode node : content) {
            JsonNode roles = node.path("roles");
            if (roles.isArray()) {
                for (JsonNode role : roles) {
                    if ("PARTNER_ADMIN".equals(role.asText())) {
                        return node.path("email").asText();
                    }
                }
            }
        }
        throw new IllegalStateException("Partner admin not found in staff list");
    }
}
