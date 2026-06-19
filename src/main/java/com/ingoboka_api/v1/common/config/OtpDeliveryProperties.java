package com.ingoboka_api.v1.common.config;

import com.ingoboka_api.v1.common.enums.OtpDeliveryChannel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ingoboka.security.otp")
public class OtpDeliveryProperties {

  /** How signup OTP codes are delivered. Use EMAIL when SMS is not configured. */
  private OtpDeliveryChannel deliveryChannel = OtpDeliveryChannel.EMAIL;
}
