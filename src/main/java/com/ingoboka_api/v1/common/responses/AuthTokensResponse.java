package com.ingoboka_api.v1.common.responses;

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
    long expiresIn;
    UserSummaryResponse user;

    @Value
    @Builder
    public static class UserSummaryResponse {
        UUID id;
        String email;
        String firstName;
        String lastName;
        String fullName;
        String phone;
        String status;
        Set<String> roles;
        String role;
        UUID organizationId;
        boolean verified;
        boolean consentGiven;
        boolean mustChangePassword;
        boolean emailVerified;
        boolean requiresEmailVerification;
        boolean accountActive;
    }
}
