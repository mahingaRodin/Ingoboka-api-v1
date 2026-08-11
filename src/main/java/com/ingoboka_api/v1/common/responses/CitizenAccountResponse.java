package com.ingoboka_api.v1.common.responses;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CitizenAccountResponse {
    UUID id;
    String email;
    String phoneNumber;
    String status;
    boolean emailVerified;
    boolean requiresEmailVerification;
}
