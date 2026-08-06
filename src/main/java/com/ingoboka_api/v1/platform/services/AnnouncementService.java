package com.ingoboka_api.v1.platform.services;

import com.ingoboka_api.v1.common.responses.AnnouncementResponse;
import java.util.List;
import java.util.UUID;

public interface AnnouncementService {

    List<AnnouncementResponse> listActiveForCitizen();

    AnnouncementResponse createPlatformAnnouncement(String title, String body);

    AnnouncementResponse createInsurerAnnouncement(UUID organizationId, String title, String body);
}
