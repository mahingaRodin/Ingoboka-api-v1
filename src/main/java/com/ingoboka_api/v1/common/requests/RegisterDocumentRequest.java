package com.ingoboka_api.v1.common.requests;

import com.ingoboka_api.v1.common.enums.DocumentAccessClassification;
import com.ingoboka_api.v1.common.enums.DocumentEntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;
import lombok.Data;

@Data
public class RegisterDocumentRequest {

    @NotNull
    private DocumentEntityType entityType;

    @NotNull
    private UUID entityId;

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
