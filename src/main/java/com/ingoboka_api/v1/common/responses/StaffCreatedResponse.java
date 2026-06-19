package com.ingoboka_api.v1.common.responses;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StaffCreatedResponse {
    UUID userId;
    String email;
    String roleCode;
    UUID organizationId;
    boolean activationRequired;
    boolean mustChangePassword;
}
