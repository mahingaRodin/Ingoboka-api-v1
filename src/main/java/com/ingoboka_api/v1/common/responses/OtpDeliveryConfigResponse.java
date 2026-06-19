package com.ingoboka_api.v1.common.responses;

import com.ingoboka_api.v1.common.config.OtpDeliveryProperties;
import com.ingoboka_api.v1.common.enums.OtpDeliveryChannel;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OtpDeliveryConfigResponse {

    OtpDeliveryChannel deliveryChannel;
    boolean requiresEmail;
    boolean smsAvailable;
    String verifyHint;

    public static OtpDeliveryConfigResponse from(OtpDeliveryProperties properties) {
        OtpDeliveryChannel channel = properties.getDeliveryChannel();
        return OtpDeliveryConfigResponse.builder()
                .deliveryChannel(channel)
                .requiresEmail(channel == OtpDeliveryChannel.EMAIL)
                .smsAvailable(channel == OtpDeliveryChannel.SMS)
                .verifyHint(switch (channel) {
                    case EMAIL -> "Enter the 6-digit code sent to your email. Login still uses your phone number.";
                    case SMS -> "Enter the 6-digit code sent to your phone by SMS.";
                    case LOG -> "OTP is logged on the API server only (development mode).";
                })
                .build();
    }
}
