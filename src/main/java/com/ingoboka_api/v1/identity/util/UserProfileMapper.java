package com.ingoboka_api.v1.identity.util;

import com.ingoboka_api.v1.identity.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileMapper {

    private final ProfilePictureUrlBuilder profilePictureUrlBuilder;

    public String resolveProfilePictureUrl(User user) {
        return profilePictureUrlBuilder.resolveProfilePictureUrl(user);
    }
}
