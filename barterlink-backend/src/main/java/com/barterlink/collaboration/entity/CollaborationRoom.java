package com.barterlink.collaboration.entity;

import com.barterlink.collaboration.enums.CollaborationRoomStatus;
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
        name = "collaboration_rooms"
)
public class CollaborationRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "room_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "session_id",
            nullable = false,
            unique = true
    )
    private ServiceSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CollaborationRoomStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant activatedAt;

    private Instant closedAt;

    @PrePersist
    protected void onCreate() {

        createdAt = Instant.now();

        if (status == null) {

            status = CollaborationRoomStatus.CREATED;

        }

    }

}