package com.ingoboka_api.v1.platform.impls;

import com.ingoboka_api.v1.common.responses.AnnouncementResponse;
import com.ingoboka_api.v1.platform.models.PlatformAnnouncement;
import com.ingoboka_api.v1.platform.repositories.PlatformAnnouncementRepository;
import com.ingoboka_api.v1.platform.services.AnnouncementService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private static final Duration DEFAULT_VISIBILITY = Duration.ofMinutes(30);

    private final PlatformAnnouncementRepository announcementRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> listActiveForCitizen() {
        Instant now = Instant.now();
        Instant freshSince = now.minus(DEFAULT_VISIBILITY);
        return announcementRepository.findActiveAnnouncements(now, freshSince).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public AnnouncementResponse createPlatformAnnouncement(String title, String body) {
        return toResponse(save("PLATFORM", null, title, body, 10));
    }

    @Override
    @Transactional
    public AnnouncementResponse createInsurerAnnouncement(UUID organizationId, String title, String body) {
        return toResponse(save("INSURER", organizationId, title, body, 5));
    }

    private PlatformAnnouncement save(
            String source, UUID organizationId, String title, String body, int priority) {
        Instant now = Instant.now();
        PlatformAnnouncement announcement = new PlatformAnnouncement();
        announcement.setId(UUID.randomUUID());
        announcement.setTitle(title);
        announcement.setBody(body);
        announcement.setSource(source);
        announcement.setOrganizationId(organizationId);
        announcement.setPriority(priority);
        announcement.setActive(true);
        announcement.setCreatedAt(now);
        announcement.setExpiresAt(now.plus(DEFAULT_VISIBILITY));
        return announcementRepository.save(announcement);
    }

    private AnnouncementResponse toResponse(PlatformAnnouncement announcement) {
        return AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .body(announcement.getBody())
                .source(announcement.getSource())
                .organizationId(announcement.getOrganizationId())
                .createdAt(announcement.getCreatedAt())
                .expiresAt(announcement.getExpiresAt())
                .build();
    }
}
