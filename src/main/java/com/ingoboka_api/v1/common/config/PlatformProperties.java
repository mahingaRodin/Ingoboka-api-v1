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
    private String brandLogoUrl =
            "https://ingoboka-platform.vercel.app/images/brand/ingoboka-logo.svg";
}
