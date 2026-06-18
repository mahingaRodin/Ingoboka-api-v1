package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.DocumentAccessClassification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class AttachPolicyDocumentRequest {

    @NotBlank
    private String documentType;

    @NotBlank
    private String objectKey;

    @NotBlank
    private String mimeType;

    @NotNull
    @Positive
    private Long sizeBytes;

    @NotBlank
    private String checksum;

    private DocumentAccessClassification accessClassification;
}
