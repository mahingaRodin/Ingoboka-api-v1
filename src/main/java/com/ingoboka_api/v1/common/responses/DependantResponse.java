package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.DependantRelationship;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DependantResponse {
    UUID id;
    String firstName;
    String lastName;
    DependantRelationship relationship;
    LocalDate dateOfBirth;
    Instant createdAt;
}
