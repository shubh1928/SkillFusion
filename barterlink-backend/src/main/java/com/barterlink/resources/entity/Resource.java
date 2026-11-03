package com.barterlink.resources.entity;

import com.barterlink.auth.entities.User;
import com.barterlink.resources.enums.ResourceCategory;
import com.barterlink.resources.enums.ResourceVisibility;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "resources")
public class Resource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "resource_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceVisibility visibility;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, unique = true, length = 255)
    private String cid;

    @Column(nullable = false, unique = true, length = 255)
    private String pinataFileId;

    @Builder.Default
    @Column(nullable = false)
    private Integer downloadCount = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    protected void onCreate() {

        Instant now = Instant.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {

        updatedAt = Instant.now();
    }

}