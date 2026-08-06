package com.ingoboka_api.v1.platform.controllers;

import com.ingoboka_api.v1.common.responses.AnnouncementResponse;
import com.ingoboka_api.v1.common.responses.ApiResponse;
import com.ingoboka_api.v1.platform.services.AnnouncementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/announcements")
@Tag(name = "Announcements", description = "Platform and insurer news banners")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "List active announcement banners for the signed-in user")
    public ApiResponse<List<AnnouncementResponse>> listActive() {
        return ApiResponse.ok("Active announcements", announcementService.listActiveForCitizen());
    }
}
