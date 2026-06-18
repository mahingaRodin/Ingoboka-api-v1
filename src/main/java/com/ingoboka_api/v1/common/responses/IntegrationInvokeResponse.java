package com.ingoboka_api.v1.common.responses;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IntegrationInvokeResponse {
    String adapterCode;
    String status;
    String message;
    Map<String, Object> result;
}
