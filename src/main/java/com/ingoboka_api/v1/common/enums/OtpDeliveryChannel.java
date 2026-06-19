package com.ingoboka_api.v1.common.enums;

public enum OtpDeliveryChannel {
  /** OTP sent by email (default when SMS is unavailable). */
  EMAIL,
  /** OTP sent via SMS adapter (MTN bulk or log fallback). */
  SMS,
  /** OTP written to API logs only — local/dev use. */
  LOG
}
