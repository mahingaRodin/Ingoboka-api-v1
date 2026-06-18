package com.ingoboka_api.v1.identity.impls;

import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.identity.services.OtpService;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "ingoboka.security.otp.storage", havingValue = "redis", matchIfMissing = true)
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    @Value("${ingoboka.security.otp.expiration-minutes:10}")
    private int expirationMinutes;

    @Override
    public String generateAndStore(String purpose, String destination) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate
                .opsForValue()
                .set(key(purpose, destination), otp, Duration.ofMinutes(expirationMinutes));
        return otp;
    }

    @Override
    public boolean verify(String purpose, String destination, String otp) {
        String stored = redisTemplate.opsForValue().get(key(purpose, destination));
        if (stored == null) {
            throw new BusinessException("OTP expired or not found");
        }
        if (!stored.equals(otp.trim())) {
            throw new BusinessException("Invalid OTP");
        }
        redisTemplate.delete(key(purpose, destination));
        return true;
    }

    @Override
    public void clear(String purpose, String destination) {
        redisTemplate.delete(key(purpose, destination));
    }

    private String key(String purpose, String destination) {
        return "otp:" + purpose + ":" + destination;
    }
}
