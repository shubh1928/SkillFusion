package com.barterlink.ratings.entity;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.auth.entities.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@Entity
@Table(name = "ratings")
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "rating_id",
            nullable = false,
            updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "agreement_id",
            nullable = false,
            unique = true
    )
    private ServiceAgreement agreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "provider_id",
            nullable = false
    )
    private User provider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "reviewer_id",
            nullable = false
    )
    private User reviewer;

    @Column(nullable = false)
    private Integer stars;

    @Column(columnDefinition = "TEXT")
    @Size(max = 1000)
    private String review;

    @Column(nullable = false,
            updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
