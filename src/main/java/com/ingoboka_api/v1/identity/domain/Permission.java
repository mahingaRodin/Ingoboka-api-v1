package com.ingoboka_api.v1.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String code;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, length = 64)
    private String module;

    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
