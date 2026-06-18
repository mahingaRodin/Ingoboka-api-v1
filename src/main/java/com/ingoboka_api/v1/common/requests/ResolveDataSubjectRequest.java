package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.DataSubjectRequestStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class ResolveDataSubjectRequest {

    @NotNull
    private UUID requestId;

    @NotNull
    private DataSubjectRequestStatus status;

    private String resolutionNotes;
}
