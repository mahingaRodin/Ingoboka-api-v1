package com.ingoboka_api.v1.common.requests;

import java.util.Map;
import lombok.Data;

@Data
public class InvokeIntegrationRequest {
    private Map<String, Object> payload;
}
