package com.barterlink.sessions.dto.response;

import com.barterlink.sessions.entity.SessionRecording;
import com.barterlink.sessions.enums.SessionRecordingStatus;

import java.time.Instant;
import java.util.UUID;

public record SessionRecordingResponseDto(
        UUID recordingId,
        UUID sessionId,
        String fileName,
        String mimeType,
        SessionRecordingStatus status,
        Long fileSizeBytes,
        Long durationSeconds,
        Instant createdAt,
        Instant uploadedAt,
        boolean educationalReusePermitted
) {

    public static SessionRecordingResponseDto from(
            SessionRecording recording
    ) {
        return new SessionRecordingResponseDto(
                recording.getId(),
                recording.getSession().getId(),
                recording.getFileName(),
                recording.getMimeType(),
                recording.getStatus(),
                recording.getFileSizeBytes(),
                recording.getDurationSeconds(),
                recording.getCreatedAt(),
                recording.getUploadedAt(),
                recording.isEducationalReusePermitted()
        );
    }
}