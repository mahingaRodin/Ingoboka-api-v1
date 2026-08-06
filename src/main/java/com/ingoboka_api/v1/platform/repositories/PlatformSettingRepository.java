package com.ingoboka_api.v1.platform.repositories;

import com.ingoboka_api.v1.platform.models.PlatformSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformSettingRepository extends JpaRepository<PlatformSetting, String> {}
