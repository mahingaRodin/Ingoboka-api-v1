package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.DependantRelationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Data;

@Data
public class CreateDependantRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotNull(message = "Relationship is required")
    private DependantRelationship relationship;

    private LocalDate dateOfBirth;

    private String nationalId;
}
