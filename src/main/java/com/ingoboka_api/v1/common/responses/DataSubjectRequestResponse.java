package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.DataSubjectRequestStatus;
import com.ingoboka_api.v1.common.enums.DataSubjectRequestType;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DataSubjectRequestResponse {
    UUID id;
    UUID userId;
    DataSubjectRequestType requestType;
    DataSubjectRequestStatus status;
    String details;
    Instant resolvedAt;
    Instant createdAt;
}
