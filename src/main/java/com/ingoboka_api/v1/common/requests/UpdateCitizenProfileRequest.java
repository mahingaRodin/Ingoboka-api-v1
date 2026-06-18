package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class UpdateCitizenProfileRequest {

    private String nationalId;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    private String gender;

    private String addressLine;

    private String district;

    private String country;

    private String occupation;

    private String preferredLanguage;
}
