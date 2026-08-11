package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class UpdateCitizenAccountRequest {

    @Email(message = "Email must be valid")
    private String email;
}
