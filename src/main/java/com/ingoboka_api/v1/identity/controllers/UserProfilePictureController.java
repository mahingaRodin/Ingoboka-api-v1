package com.ingoboka_api.v1.identity.controllers;

import com.ingoboka_api.v1.common.requests.SetProfilePictureUrlRequest;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.common.responses.ProfilePictureResponse;
import com.ingoboka_api.v1.identity.services.UserProfilePictureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/users/me/profile-picture")
@RequiredArgsConstructor
@Tag(name = "Profile picture", description = "Optional profile photo for all authenticated users")
@SecurityRequirement(name = "bearerAuth")
public class UserProfilePictureController {

    private final UserProfilePictureService userProfilePictureService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get my profile picture URL")
    public ApiResponse<ProfilePictureResponse> getMyProfilePicture() {
        return ApiResponse.ok("Profile picture retrieved", userProfilePictureService.getMyProfilePicture());
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Set profile picture from external URL")
    public ApiResponse<ProfilePictureResponse> setProfilePictureUrl(
            @Valid @RequestBody SetProfilePictureUrlRequest request) {
        ProfilePictureResponse response = userProfilePictureService.setFromUrl(request.getProfilePictureUrl());
        return ApiResponse.ok("Profile picture updated", response);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload profile picture file")
    public ApiResponse<ProfilePictureResponse> uploadProfilePicture(@RequestPart("file") MultipartFile file) {
        ProfilePictureResponse response = userProfilePictureService.upload(file);
        return ApiResponse.ok("Profile picture uploaded", response);
    }

    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Remove profile picture")
    public ApiResponse<ProfilePictureResponse> removeProfilePicture() {
        ProfilePictureResponse response = userProfilePictureService.remove();
        return ApiResponse.ok("Profile picture removed", response);
    }
}
