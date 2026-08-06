package com.ingoboka_api.v1.platform.repositories;

import com.ingoboka_api.v1.platform.models.PlatformAnnouncement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlatformAnnouncementRepository extends JpaRepository<PlatformAnnouncement, UUID> {

    @Query("""
            SELECT a FROM PlatformAnnouncement a
            WHERE a.active = true
              AND (a.expiresAt IS NULL OR a.expiresAt > :now)
              AND (a.createdAt >= :freshSince OR a.expiresAt IS NULL)
            ORDER BY a.priority DESC, a.createdAt DESC
            """)
    List<PlatformAnnouncement> findActiveAnnouncements(
            @Param("now") Instant now, @Param("freshSince") Instant freshSince);
}
