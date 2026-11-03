
package com.barterlink.inactivity.entity;

import com.barterlink.auth.entities.Provider;
import com.barterlink.auth.entities.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "retained_user_identities",
        indexes = {
                @Index(
                        name = "idx_retained_identity_expiry",
                        columnList = "expires_at"
                )
        }
)
public class RetainedUserIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "retained_identity_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            updatable = false
    )
    private User user;

    @Column(name = "original_email", length = 300)
    private String originalEmail;

    @Column(name = "original_name", length = 500)
    private String originalName;

    @Column(name = "original_image")
    private String originalImage;

    @Enumerated(EnumType.STRING)
    @Column(name = "original_provider", length = 20)
    private Provider originalProvider;

    @Column(name = "original_provider_id")
    private String originalProviderId;

    @Column(name = "original_bio", columnDefinition = "TEXT")
    private String originalBio;

    @Column(name = "retained_at", nullable = false, updatable = false)
    private Instant retainedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
}
