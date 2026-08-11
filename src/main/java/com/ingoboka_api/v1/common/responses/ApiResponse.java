package com.ingoboka_api.v1.common.responses;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ApiResponse<T> {

    boolean success;
    String message;
    String code;
    Map<String, String> fieldErrors;
    T data;
    Instant timestamp;

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String code) {
        return error(message, code, null);
    }

    public static <T> ApiResponse<T> error(String message, String code, Map<String, String> fieldErrors) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .code(code)
                .fieldErrors(fieldErrors)
                .timestamp(Instant.now())
                .build();
    }
}
