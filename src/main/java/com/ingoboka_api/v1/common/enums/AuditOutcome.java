package com.ingoboka_api.v1.common.enums;

import java.util.Locale;
import java.util.Set;

public enum AuditOutcome {
    SUCCESS,
    FAILED,
    PENDING,
    INFO;

    private static final Set<String> LEGACY_FAILURE = Set.of("FAILURE", "FAILED");

    public static AuditOutcome from(String raw) {
        if (raw == null || raw.isBlank()) {
            return SUCCESS;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        if (LEGACY_FAILURE.contains(normalized)) {
            return FAILED;
        }
        try {
            return AuditOutcome.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return SUCCESS;
        }
    }

    public String value() {
        return name();
    }
}
