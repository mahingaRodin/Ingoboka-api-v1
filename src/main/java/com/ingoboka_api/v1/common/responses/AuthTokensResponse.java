package com.ingoboka_api.v1.identity.dto;

import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthTokensResponse {

    String accessToken;
    String refreshToken;
    String tokenType;
    long expiresInMinutes;
    UserSummaryResponse user;

    @Value
    @Builder
    public static class UserSummaryResponse {
        UUID id;
        String email;
        String firstName;
        String lastName;
        String status;
        Set<String> roles;
        UUID organizationId;
    }
}
