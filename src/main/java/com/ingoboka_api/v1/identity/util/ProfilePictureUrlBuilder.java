package com.ingoboka_api.v1.identity.util;

import com.ingoboka_api.v1.identity.models.User;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ProfilePictureUrlBuilder {

    public static final String CONTENT_PATH = "/api/v1/users/me/profile-picture/content";

    public String resolveProfilePictureUrl(User user) {
        if (user == null || user.getProfilePictureKey() == null || user.getProfilePictureKey().isBlank()) {
            return null;
        }
        String key = user.getProfilePictureKey().trim();
        if (isHttpUrl(key)) {
            return key;
        }
        long version = user.getUpdatedAt() != null ? user.getUpdatedAt().toEpochMilli() : Instant.now().toEpochMilli();
        return CONTENT_PATH + "?v=" + version;
    }

    private static boolean isHttpUrl(String value) {
        String lower = value.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }
}
