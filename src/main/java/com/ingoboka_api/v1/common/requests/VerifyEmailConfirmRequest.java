package com.ingoboka_api.v1.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailConfirmRequest {

    @NotBlank(message = "Token is required")
    private String token;
}
