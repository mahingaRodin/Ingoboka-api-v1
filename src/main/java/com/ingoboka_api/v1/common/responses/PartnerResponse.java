package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.OrganizationStatus;
import com.ingoboka_api.v1.common.enums.OrganizationType;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PartnerResponse {
    UUID id;
    String name;
    String code;
    OrganizationType type;
    OrganizationStatus status;
    String registrationNumber;
    String contactEmail;
    String contactPhone;
    String addressLine;
    String district;
    String country;
    String website;
    Instant createdAt;
    Instant updatedAt;
}
