package com.ingoboka_api.v1.identity.impls;

import com.ingoboka_api.v1.audit.services.AuditComplianceService;
import com.ingoboka_api.v1.common.exception.BusinessException;
import com.ingoboka_api.v1.common.responses.ProfilePictureResponse;
import com.ingoboka_api.v1.common.security.SecurityUtils;
import com.ingoboka_api.v1.document.services.DocumentStorageService;
import com.ingoboka_api.v1.identity.model.ProfilePictureContent;
import com.ingoboka_api.v1.identity.models.User;
import com.ingoboka_api.v1.identity.repositories.UserRepository;
import com.ingoboka_api.v1.identity.services.UserProfilePictureService;
import com.ingoboka_api.v1.identity.util.UserProfileMapper;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserProfilePictureServiceImpl implements UserProfilePictureService {

    private static final long MAX_BYTES = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    private final UserRepository userRepository;
    private final DocumentStorageService documentStorageService;
    private final UserProfileMapper userProfileMapper;
    private final AuditComplianceService auditComplianceService;

    @Override
    @Transactional(readOnly = true)
    public ProfilePictureResponse getMyProfilePicture() {
        User user = requireCurrentUser();
        return toResponse(user);
    }

    @Override
    @Transactional
    public ProfilePictureResponse setFromUrl(String profilePictureUrl) {
        User user = requireCurrentUser();
        applyProfilePictureUrl(user.getId(), profilePictureUrl);
        User refreshed = requireCurrentUser();
        auditComplianceService.log(
                "PROFILE_PICTURE_UPDATED", "USER", refreshed.getId(), "Profile picture URL updated");
        return toResponse(refreshed);
    }

    @Override
    @Transactional
    public ProfilePictureResponse upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Profile picture file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("Profile picture must be 2 MB or smaller");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("Profile picture must be JPEG, PNG, WebP, or GIF");
        }

        User user = requireCurrentUser();
        deleteStoredObject(user.getProfilePictureKey());

        String extension = extensionFor(contentType);
        String objectKey = "users/" + user.getId() + "/profile-" + UUID.randomUUID() + extension;
        try {
            documentStorageService.upload(objectKey, file.getInputStream(), file.getSize(), contentType);
        } catch (IOException ex) {
            throw new BusinessException("Failed to read profile picture upload");
        }

        user.setProfilePictureKey(objectKey);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        auditComplianceService.log(
                "PROFILE_PICTURE_UPDATED", "USER", user.getId(), "Profile picture uploaded");
        return toResponse(user);
    }

    @Override
    @Transactional
    public ProfilePictureResponse remove() {
        User user = requireCurrentUser();
        deleteStoredObject(user.getProfilePictureKey());
        user.setProfilePictureKey(null);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
        auditComplianceService.log(
                "PROFILE_PICTURE_REMOVED", "USER", user.getId(), "Profile picture removed");
        return ProfilePictureResponse.builder().profilePictureUrl(null).build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfilePictureContent openMyProfilePictureContent() {
        User user = requireCurrentUser();
        String key = user.getProfilePictureKey();
        if (!StringUtils.hasText(key)) {
            return null;
        }
        String trimmed = key.trim();
        if (isHttpUrl(trimmed)) {
            return new ProfilePictureContent.ProfilePictureExternalRedirect(java.net.URI.create(trimmed));
        }
        return new ProfilePictureContent.ProfilePictureStoredObject(documentStorageService.open(trimmed));
    }

    @Override
    @Transactional
    public void applyProfilePictureUrl(UUID userId, String profilePictureUrl) {
        if (!StringUtils.hasText(profilePictureUrl)) {
            return;
        }
        String trimmed = profilePictureUrl.trim();
        if (!isHttpUrl(trimmed)) {
            throw new BusinessException("Profile picture URL must start with http:// or https://");
        }
        User user = userRepository
                .findWithDetailsById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        deleteStoredObject(user.getProfilePictureKey());
        user.setProfilePictureKey(trimmed);
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);
    }

    private User requireCurrentUser() {
        return userRepository
                .findWithDetailsById(SecurityUtils.currentUser().getUserId())
                .orElseThrow(() -> new BusinessException("User not found"));
    }

    private ProfilePictureResponse toResponse(User user) {
        return ProfilePictureResponse.builder()
                .profilePictureUrl(userProfileMapper.resolveProfilePictureUrl(user))
                .build();
    }

    private void deleteStoredObject(String key) {
        if (!StringUtils.hasText(key) || isHttpUrl(key)) {
            return;
        }
        try {
            if (documentStorageService.exists(key)) {
                documentStorageService.delete(key);
            }
        } catch (RuntimeException ex) {
            // Best-effort cleanup; new picture still applies.
        }
    }

    private static boolean isHttpUrl(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private static String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "application/octet-stream";
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    private static String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
