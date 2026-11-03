package com.barterlink.sessions.repository;

import com.barterlink.sessions.entity.SessionRecordingConsent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRecordingConsentRepository
        extends JpaRepository<SessionRecordingConsent, UUID> {

    Optional<SessionRecordingConsent>
    findTopBySession_IdAndUser_IdAndModeAgreedAtOrderByDecidedAtDesc(
            UUID sessionId,
            UUID userId,
            Instant modeAgreedAt);

    List<SessionRecordingConsent>
    findBySession_IdOrderByDecidedAtDesc(
            UUID sessionId);
}