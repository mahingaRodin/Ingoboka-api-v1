package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.UserStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ManagedUserResponse {

    UUID id;
    String email;
    String phoneNumber;
    String firstName;
    String lastName;
    UserStatus status;
    boolean emailVerified;
    boolean phoneVerified;
    boolean mustChangePassword;
    UUID organizationId;
    String organizationName;
    Set<String> roles;
    Instant createdAt;
    Instant updatedAt;
}
