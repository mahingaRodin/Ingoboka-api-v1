package com.ingoboka_api.v1.common.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    /** External image URL or MinIO object key for product hero image. */
    private String heroImageUrl;
}
