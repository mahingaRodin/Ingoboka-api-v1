package com.ingoboka_api.v1.common.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public class PhoneNumberUtils {

  private static final String RWANDA_COUNTRY_CODE = "250";

  /**
   * Normalizes Rwanda mobile numbers to E.164 ({@code +2507XXXXXXXX}).
   * Accepts local ({@code 078...}), country-code ({@code 25078...}), and E.164 forms.
   */
  public static String normalizeRwanda(String phone) {
    if (phone == null || phone.isBlank()) {
      return "";
    }

    String digits = phone.trim().replaceAll("[^0-9]", "");
    if (digits.isEmpty()) {
      return phone.trim();
    }

    if (digits.startsWith("0") && digits.length() == 10) {
      digits = RWANDA_COUNTRY_CODE + digits.substring(1);
    } else if (digits.length() == 9 && digits.startsWith("7")) {
      digits = RWANDA_COUNTRY_CODE + digits;
    }

    if (digits.startsWith(RWANDA_COUNTRY_CODE) && digits.length() == 12) {
      return "+" + digits;
    }

    return phone.trim();
  }

  public static boolean looksLikeEmail(String value) {
    return value != null && value.contains("@");
  }

  public static boolean isPlaceholderIdentifier(String value) {
    if (value == null || value.isBlank()) {
      return false;
    }
    String trimmed = value.trim();
    return "email".equalsIgnoreCase(trimmed) || "phone".equalsIgnoreCase(trimmed);
  }
}
