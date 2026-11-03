package com.barterlink.negotiation.entity;

import com.barterlink.auth.entities.User;
import com.barterlink.negotiation.enums.NegotiationMessageType;
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
@Table(name = "negotiation_messages")
public class NegotiationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(
            name = "message_id",
            nullable = false,
            updatable = false
    )
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "negotiation_id",
            nullable = false
    )
    private Negotiation negotiation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "sender_id"
    )
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NegotiationMessageType messageType;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(length = 1000)
    private String attachmentUrl;

    @Builder.Default
    @Column(nullable = false)
    private Boolean systemGenerated = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {

        createdAt = Instant.now();

        if (messageType == null) {
            messageType = NegotiationMessageType.TEXT;
        }
    }
}