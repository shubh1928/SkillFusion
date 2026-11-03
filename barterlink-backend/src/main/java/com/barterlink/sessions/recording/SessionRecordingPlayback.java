package com.barterlink.sessions.recording;

public record SessionRecordingPlayback(
        String googleDriveFileId,
        String fileName,
        String mimeType,
        Long fileSizeBytes
) {
}