package com.ingoboka_api.v1.support;

import com.ingoboka_api.v1.identity.impls.InMemoryOtpServiceImpl;
import com.ingoboka_api.v1.identity.services.OtpService;
import java.lang.reflect.Field;
import java.util.Map;

public final class OtpTestSupport {

    private OtpTestSupport() {}

    public static String latestOtpFor(OtpService otpService, String purpose, String destination) {
        if (!(otpService instanceof InMemoryOtpServiceImpl inMemoryOtpService)) {
            throw new IllegalStateException("OTP test support requires ingoboka.security.otp.storage=memory");
        }
        try {
            Field storeField = InMemoryOtpServiceImpl.class.getDeclaredField("store");
            storeField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ?> store = (Map<String, ?>) storeField.get(inMemoryOtpService);
            String key = purpose + ":" + destination;
            Object entry = store.get(key);
            if (entry == null) {
                throw new IllegalStateException("No OTP stored for " + key);
            }
            Field otpField = entry.getClass().getDeclaredField("otp");
            otpField.setAccessible(true);
            return (String) otpField.get(entry);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to read in-memory OTP", ex);
        }
    }
}
