package com.ingoboka_api.v1.common.requests;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class LoginRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ignoresSwaggerPlaceholderIdentifierAndUsesEmailField() throws Exception {
        LoginRequest request = objectMapper.readValue(
                """
                {
                  "identifier": "email",
                  "email": "user@example.com",
                  "password": "Secret@123"
                }
                """,
                LoginRequest.class);

        assertThat(request.resolvedIdentifier()).isEqualTo("user@example.com");
    }

    @Test
    void normalizesPhoneIdentifier() throws Exception {
        LoginRequest request = objectMapper.readValue(
                """
                {
                  "identifier": "0781234567",
                  "password": "Secret@123"
                }
                """,
                LoginRequest.class);

        assertThat(request.resolvedIdentifier()).isEqualTo("+250781234567");
    }

    @Test
    void usesPhoneFieldWhenIdentifierMissing() throws Exception {
        LoginRequest request = objectMapper.readValue(
                """
                {
                  "phone": "+250780000001",
                  "password": "Secret@123"
                }
                """,
                LoginRequest.class);

        assertThat(request.resolvedIdentifier()).isEqualTo("+250780000001");
    }
}
