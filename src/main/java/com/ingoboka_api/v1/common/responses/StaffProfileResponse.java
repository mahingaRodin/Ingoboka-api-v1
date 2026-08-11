package com.ingoboka_api.v1.common.responses;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StaffProfileResponse {
    UUID id;
    String email;
    String phoneNumber;
    String firstName;
    String lastName;
    String status;
    boolean emailVerified;
    boolean requiresEmailVerification;
    Set<String> roles;
    UUID organizationId;
    String organizationName;
    String profilePictureUrl;
    Instant createdAt;
    Instant updatedAt;
}
