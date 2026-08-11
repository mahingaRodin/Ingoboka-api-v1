package com.ingoboka_api.v1.identity.services;

import com.ingoboka_api.v1.common.responses.ProfilePictureResponse;
import com.ingoboka_api.v1.identity.model.ProfilePictureContent;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfilePictureService {

    ProfilePictureResponse getMyProfilePicture();

    ProfilePictureResponse setFromUrl(String profilePictureUrl);

    ProfilePictureResponse upload(MultipartFile file);

    ProfilePictureResponse remove();

    ProfilePictureContent openMyProfilePictureContent();

    void applyProfilePictureUrl(UUID userId, String profilePictureUrl);
}
