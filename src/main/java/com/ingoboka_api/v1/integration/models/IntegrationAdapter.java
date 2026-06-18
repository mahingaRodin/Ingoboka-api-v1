package com.ingoboka_api.v1.integration.models;

import com.ingoboka_api.v1.common.enums.IntegrationAdapterType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "integration_adapters")
public class IntegrationAdapter {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "adapter_type", nullable = false, length = 64)
    private IntegrationAdapterType adapterType;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "config_json")
    private String configJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
