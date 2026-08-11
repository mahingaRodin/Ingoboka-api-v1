package com.ingoboka_api.v1.common.exception;

import java.util.Collections;
import java.util.Map;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final Map<String, String> fieldErrors;

    public BusinessException(String message) {
        this(message, null, null);
    }

    public BusinessException(String message, String code) {
        this(message, code, null);
    }

    public BusinessException(String message, String code, Map<String, String> fieldErrors) {
        super(message);
        this.code = code;
        this.fieldErrors = fieldErrors != null ? Map.copyOf(fieldErrors) : Collections.emptyMap();
    }
}
