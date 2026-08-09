package com.ingoboka_api.v1.common.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CountryCodes {

    private static final String DEFAULT = "RW";

    /**
     * Normalizes country input to ISO 3166-1 alpha-2 for profile storage ({@code VARCHAR(2)}).
     * Maps {@code Rwanda} (any case) to {@code RW}; two-letter codes are uppercased; blank defaults to {@code RW}.
     */
    public static String normalizeRwandaAware(String country) {
        if (country == null || country.isBlank()) {
            return DEFAULT;
        }
        String trimmed = country.trim();
        if ("rwanda".equalsIgnoreCase(trimmed)) {
            return DEFAULT;
        }
        if (trimmed.length() == 2) {
            return trimmed.toUpperCase();
        }
        return DEFAULT;
    }
}
