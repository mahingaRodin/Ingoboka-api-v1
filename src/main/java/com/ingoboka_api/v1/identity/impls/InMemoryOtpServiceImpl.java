package com.ingoboka_api.v1.identity.impls;

import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.identity.services.OtpService;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "ingoboka.security.otp.storage", havingValue = "memory")
public class InMemoryOtpServiceImpl implements OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final Map<String, OtpEntry> store = new ConcurrentHashMap<>();

    @Value("${ingoboka.security.otp.expiration-minutes:10}")
    private int expirationMinutes;

    @Override
    public String generateAndStore(String purpose, String destination) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));
        store.put(key(purpose, destination), new OtpEntry(otp, Instant.now().plusSeconds(expirationMinutes * 60L)));
        return otp;
    }

    @Override
    public boolean verify(String purpose, String destination, String otp) {
        OtpEntry entry = store.get(key(purpose, destination));
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            store.remove(key(purpose, destination));
            throw new BusinessException("OTP expired or not found");
        }
        if (!entry.otp().equals(otp.trim())) {
            throw new BusinessException("Invalid OTP");
        }
        store.remove(key(purpose, destination));
        return true;
    }

    @Override
    public void clear(String purpose, String destination) {
        store.remove(key(purpose, destination));
    }

    private String key(String purpose, String destination) {
        return purpose + ":" + destination;
    }

    private record OtpEntry(String otp, Instant expiresAt) {}
}
