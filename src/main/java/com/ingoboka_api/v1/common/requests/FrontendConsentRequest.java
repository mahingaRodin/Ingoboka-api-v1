package com.ingoboka_api.v1.common.requests;

import lombok.Data;

@Data
public class FrontendConsentRequest {

    private boolean dataProcessing;
    private boolean marketing;
    private boolean termsAccepted;
}
