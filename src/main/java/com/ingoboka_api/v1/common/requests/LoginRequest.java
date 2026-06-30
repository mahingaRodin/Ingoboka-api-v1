package com.ingoboka_api.v1.common.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ingoboka_api.v1.common.util.PhoneNumberUtils;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login with phone or email plus password")
public class LoginRequest {

  @Schema(
      description = "Phone number (+250...) or email address",
      example = "+250780000001")
  private String identifier;

  @Schema(description = "Email address (alternative to identifier)", example = "user@example.com")
  private String email;

  @Schema(description = "Phone number (alternative to identifier)", example = "+250780000001")
  private String phone;

  @NotBlank(message = "Password is required")
  @Schema(example = "YourPassword@123")
  private String password;

  @JsonIgnore
  public String resolvedIdentifier() {
    if (isUsableCredential(identifier)) {
      return normalizeCredential(identifier);
    }
    if (isUsableCredential(email)) {
      return normalizeCredential(email);
    }
    if (isUsableCredential(phone)) {
      return normalizeCredential(phone);
    }
    throw new IllegalArgumentException("Phone or email is required");
  }

  private static boolean isUsableCredential(String value) {
    return value != null
        && !value.isBlank()
        && !PhoneNumberUtils.isPlaceholderIdentifier(value);
  }

  private static String normalizeCredential(String value) {
    String trimmed = value.trim();
    if (PhoneNumberUtils.looksLikeEmail(trimmed)) {
      return trimmed.toLowerCase();
    }
    return PhoneNumberUtils.normalizeRwanda(trimmed);
  }
}
