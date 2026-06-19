package com.ingoboka_api.v1.common.requests;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    /** Phone or email — preferred for frontend. */
  private String identifier;

    @JsonAlias("identifier")
    private String email;

    @JsonAlias("identifier")
    private String phone;

    @NotBlank(message = "Password is required")
    private String password;

    public String resolvedIdentifier() {
        if (identifier != null && !identifier.isBlank()) {
            return identifier.trim();
        }
        if (email != null && !email.isBlank()) {
            return email.trim();
        }
        if (phone != null && !phone.isBlank()) {
            return phone.trim();
        }
        throw new IllegalArgumentException("Phone or email is required");
    }
}
