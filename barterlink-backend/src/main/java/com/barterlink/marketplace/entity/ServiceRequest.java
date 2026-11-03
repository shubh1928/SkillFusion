package com.barterlink.marketplace.entity;

import com.barterlink.auth.entities.User;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.marketplace.enums.ServiceRequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "service_requests")
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "request_id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false)
    private Integer estimatedHours;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceRequestStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ExchangeMode exchangeMode;

    @Column(length = 255)
    private String offerTitle;

    @Column(columnDefinition = "TEXT")
    private String offerDescription;

    @Column(nullable =false, length = 255)
    private String requestTitle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requestDescription;

    @Column
    private Integer tokenAmount;

    @Builder.Default
    @Column(nullable = false)
    private Boolean selectiveRequest = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(
            mappedBy = "request",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private Set<RequestSkill> requiredSkills = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;

        if (status == null) {
            status = ServiceRequestStatus.OPEN;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}