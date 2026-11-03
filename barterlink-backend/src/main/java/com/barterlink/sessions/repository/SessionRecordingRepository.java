
package com.barterlink.sessions.repository;

import com.barterlink.sessions.entity.SessionRecording;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionRecordingRepository
        extends JpaRepository<SessionRecording, UUID> {

    List<SessionRecording> findBySession_IdOrderByCreatedAtDesc(
            UUID sessionId
    );

    Optional<SessionRecording> findByIdAndSession_Id(
            UUID recordingId,
            UUID sessionId
    );

    Optional<SessionRecording> findByGoogleDriveFileId(
            String googleDriveFileId
    );
}
