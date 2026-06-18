package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Data;

@Data
public class CreateRefundRequest {

    @NotNull
    private UUID paymentId;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String reason;
}
