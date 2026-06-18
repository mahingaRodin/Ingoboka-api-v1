package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.DataSubjectRequestType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitDataSubjectRequest {

    @NotNull
    private DataSubjectRequestType requestType;

    private String details;
}
