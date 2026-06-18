package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewApplicationRequest {

    @NotNull(message = "Status is required")
    private ApplicationStatus status;

    private String decisionReason;
}
