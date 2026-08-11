package com.ingoboka_api.v1.identity.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.ingoboka_api.v1.identity.models.User;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProfilePictureUrlBuilderTest {

    private final ProfilePictureUrlBuilder builder = new ProfilePictureUrlBuilder();

    @Test
    void returnsNullWhenNoKey() {
        User user = new User();
        assertThat(builder.resolveProfilePictureUrl(user)).isNull();
    }

    @Test
    void returnsExternalUrlAsIs() {
        User user = new User();
        user.setProfilePictureKey("https://cdn.example/photo.jpg");
        assertThat(builder.resolveProfilePictureUrl(user)).isEqualTo("https://cdn.example/photo.jpg");
    }

    @Test
    void returnsApiProxyUrlForStoredKey() {
        Instant updated = Instant.parse("2026-01-15T10:00:00Z");
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setProfilePictureKey("users/abc/profile.jpg");
        user.setUpdatedAt(updated);
        assertThat(builder.resolveProfilePictureUrl(user))
                .isEqualTo("/api/v1/users/me/profile-picture/content?v=" + updated.toEpochMilli());
    }
}
