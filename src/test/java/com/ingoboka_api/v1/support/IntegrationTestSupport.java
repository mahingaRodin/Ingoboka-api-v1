package com.ingoboka_api.v1.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    private static final boolean USE_LOCAL_SERVICES = Boolean.getBoolean("integration.local");

    private static PostgreSQLContainer<?> postgres;
    private static GenericContainer<?> redis;
    private static boolean containersStarted;

    @Autowired
    protected MockMvc mockMvc;

    protected final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void clearLeakedSecurityContextBeforeTest() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearLeakedSecurityContextAfterTest() {
        SecurityContextHolder.clearContext();
    }

    static boolean isEnabled() {
        if (!Boolean.getBoolean("integration")) {
            return false;
        }
        if (USE_LOCAL_SERVICES) {
            return true;
        }
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (!Boolean.getBoolean("integration")) {
            return;
        }

        if (USE_LOCAL_SERVICES) {
            registry.add(
                    "spring.datasource.url",
                    () -> System.getProperty(
                            "integration.jdbc.url", "jdbc:postgresql://localhost:5432/ingoboka"));
            registry.add(
                    "spring.datasource.username",
                    () -> System.getProperty("integration.jdbc.username", "ingoboka"));
            registry.add(
                    "spring.datasource.password",
                    () -> System.getProperty("integration.jdbc.password", "12345"));
            registry.add("spring.data.redis.host",
                    () -> System.getProperty("integration.redis.host", "localhost"));
            registry.add("spring.data.redis.port",
                    () -> Integer.parseInt(System.getProperty("integration.redis.port", "6379")));
            registry.add(
                    "ingoboka.seed.platform-admin.email",
                    () -> System.getProperty("integration.admin.email", "agressive.one04@gmail.com"));
            registry.add(
                    "ingoboka.seed.platform-admin.password",
                    () -> System.getProperty("integration.admin.password", "admin@123"));
            return;
        }

        ensureContainersStarted();
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
        registry.add("spring.data.redis.host", () -> redis.getHost());
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    private static synchronized void ensureRedisContainerStarted() {
        if (redis != null) {
            return;
        }
        redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
        redis.start();
    }

    private static synchronized void ensureContainersStarted() {
        if (containersStarted) {
            return;
        }
        postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("ingoboka_test")
                .withUsername("ingoboka")
                .withPassword("ingoboka");
        ensureRedisContainerStarted();
        postgres.start();
        containersStarted = true;
    }

    protected String platformAdminEmail() {
        return USE_LOCAL_SERVICES
                ? System.getProperty("integration.admin.email", "agressive.one04@gmail.com")
                : "platform-admin@test.ingoboka";
    }

    protected String platformAdminPassword() {
        return USE_LOCAL_SERVICES
                ? System.getProperty("integration.admin.password", "admin@123")
                : "Admin@Test123";
    }

    protected ResultActions get(String path) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path));
    }

    protected ResultActions get(String path, String bearerToken) throws Exception {
        return mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path)
                        .header("Authorization", "Bearer " + bearerToken));
    }

    protected ResultActions post(String path, String jsonBody) throws Exception {
        return mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody));
    }

    protected ResultActions post(String path, String jsonBody, String bearerToken) throws Exception {
        return mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody)
                        .header("Authorization", "Bearer " + bearerToken));
    }

    protected ResultActions put(String path, String jsonBody, String bearerToken) throws Exception {
        return mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody)
                        .header("Authorization", "Bearer " + bearerToken));
    }

    protected ResultActions patch(String path, String jsonBody, String bearerToken) throws Exception {
        return mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody)
                        .header("Authorization", "Bearer " + bearerToken));
    }

    protected ResultActions delete(String path, String bearerToken) throws Exception {
        return mockMvc.perform(
                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(path)
                        .header("Authorization", "Bearer " + bearerToken));
    }

    protected String loginAccessToken(String identifier, String password) throws Exception {
        String body = """
                {"identifier":"%s","password":"%s"}
                """.formatted(identifier, password);
        String response = post("/api/v1/auth/login", body)
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode root = objectMapper.readTree(response);
        if (!root.path("success").asBoolean(false)) {
            throw new IllegalStateException("Login failed for " + identifier + ": " + response);
        }
        return root.path("data").path("accessToken").asText();
    }

    protected String loginPlatformAdmin() throws Exception {
        return loginAccessToken(platformAdminEmail(), platformAdminPassword());
    }

    protected void assertNotServerError(ResultActions actions) throws Exception {
        actions.andExpect(result -> {
            int status = result.getResponse().getStatus();
            if (status >= 500) {
                throw new AssertionError(
                        "Expected non-5xx status but got " + status + " body="
                                + result.getResponse().getContentAsString());
            }
        });
    }

    protected String randomPhoneSuffix() {
        return String.valueOf(System.nanoTime()).substring(Math.max(0, String.valueOf(System.nanoTime()).length() - 7));
    }
}
