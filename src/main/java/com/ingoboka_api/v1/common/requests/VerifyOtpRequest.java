package com.ingoboka_api.v1.common.requests;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ingoboka_api.v1.common.util.PhoneNumberUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank
    @JsonAlias("phone")
    @Schema(example = "+250780000001")
    private String phoneNumber;

    @NotBlank
    @JsonAlias("code")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be 6 digits")
    private String otp;

    @JsonIgnore
    public String resolvedPhoneNumber() {
        return PhoneNumberUtils.normalizeRwanda(phoneNumber);
    }
}
