package com.ingoboka_api.v1.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ingoboka.security.jwt")
public class JwtProperties {

    private String secret;
    private long accessTokenExpirationMinutes = 30;
    private long refreshTokenExpirationDays = 7;
}
