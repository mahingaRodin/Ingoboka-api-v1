package com.ingoboka_api.v1.identity.util;

import com.ingoboka_api.v1.document.util.StorageUrlResolver;
import com.ingoboka_api.v1.identity.models.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileMapper {

    private final StorageUrlResolver storageUrlResolver;

    public String resolveProfilePictureUrl(User user) {
        if (user == null || user.getProfilePictureKey() == null || user.getProfilePictureKey().isBlank()) {
            return null;
        }
        return storageUrlResolver.resolveDownloadUrl(user.getProfilePictureKey());
    }
}
