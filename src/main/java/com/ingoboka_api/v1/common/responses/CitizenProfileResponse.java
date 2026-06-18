package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.KycStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CitizenProfileResponse {
    UUID id;
    UUID userId;
    LocalDate dateOfBirth;
    String gender;
    String addressLine;
    String district;
    String country;
    String occupation;
    String preferredLanguage;
    KycStatus kycStatus;
    Instant createdAt;
    Instant updatedAt;
}
