package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.enums.AppealStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClaimAppealResponse {
    UUID id;
    UUID claimId;
    String reason;
    AppealStatus status;
    Instant submittedAt;
    Instant reviewedAt;
    String reviewNotes;
}
