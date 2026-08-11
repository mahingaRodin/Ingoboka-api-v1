package com.ingoboka_api.v1.common.requests;

import lombok.Data;

@Data
public class UnpublishProductRequest {

    /** When true, moves the product to ARCHIVED instead of back to DRAFT. */
    private boolean archive = false;
}
