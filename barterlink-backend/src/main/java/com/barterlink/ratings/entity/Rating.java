package com.barterlink.ratings.entity;

import com.barterlink.auth.entities.User;
import com.barterlink.ratings.enums.RatingType;
import com.barterlink.sessions.entity.ServiceSession;
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
@Table(
        name = "ratings",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "session_id",
                                "rated_by"
                        }
                )
        }
)
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "rating_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "session_id",
            nullable = false
    )
    private ServiceSession session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "rated_by",
            nullable = false
    )
    private User ratedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "rated_user",
            nullable = false
    )
    private User ratedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RatingType ratingType;

    @Column(nullable = false)
    private Integer stars;

    @Column(columnDefinition = "TEXT")
    private String review;

    @Column(
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}