package com.ingoboka_api.v1.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ingoboka.platform")
public class PlatformProperties {

    private String name = "Ingoboka Platform";
    private String frontendLoginUrl = "http://localhost:3000/en/login";
    private String frontendVerifyEmailUrl = "http://localhost:3000/en/verify-email";
    private String frontendActivateAccountUrl = "http://localhost:3000/en/activate";
    /** Base URL with locale prefix, e.g. https://app.ingoboka.rw/en */
    private String frontendBaseUrl = "http://localhost:3000/en";
    private String brandLogoUrl =
            "https://ingoboka-platform.vercel.app/images/brand/ingoboka-logo.svg";

    /** Public policy QR verification URL shown on digital cards and PDFs. */
    public String buildPolicyVerificationUrl(String qrToken) {
        String base = frontendBaseUrl != null ? frontendBaseUrl.trim() : "http://localhost:3000/en";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/verify/" + qrToken;
    }
}
