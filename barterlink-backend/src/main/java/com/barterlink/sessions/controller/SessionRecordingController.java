package com.barterlink.sessions.controller;

import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import com.barterlink.sessions.dto.response.SessionRecordingResponseDto;
import com.barterlink.sessions.recording.SessionRecordingService;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.barterlink.sessions.recording.GoogleDriveStorageService;
import com.barterlink.sessions.recording.SessionRecordingPlayback;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.io.IOException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionRecordingController {

    private final SessionRecordingService sessionRecordingService;
    private final CurrentUserUtil currentUserUtil;
    private final GoogleDriveStorageService driveStorageService;

    @PostMapping(
            value = "/{sessionId}/recordings",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<SessionRecordingResponseDto> uploadRecording(
            @PathVariable UUID sessionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(
                    value = "durationSeconds",
                    required = false
            ) Long durationSeconds
    ) throws IOException {

        SessionRecordingResponseDto response =
                sessionRecordingService.uploadRecording(
                        sessionId,
                        currentUserUtil.getCurrentUserId(),
                        file,
                        durationSeconds
                );

        return ApiResponse.success(
                "Recording uploaded successfully",
                response
        );
    }

    @GetMapping("/{sessionId}/recordings")
    public ApiResponse<List<SessionRecordingResponseDto>> getSessionRecordings(
            @PathVariable UUID sessionId
    ) {
        List<SessionRecordingResponseDto> recordings =
                sessionRecordingService.getSessionRecordings(
                        sessionId,
                        currentUserUtil.getCurrentUserId()
                );

        return ApiResponse.success(
                "Session recordings retrieved successfully",
                recordings
        );
    }

    @GetMapping("/{sessionId}/recordings/{recordingId}/content")
    public ResponseEntity<StreamingResponseBody> streamRecording(
            @PathVariable UUID sessionId,
            @PathVariable UUID recordingId
    ) {
        SessionRecordingPlayback recording =
                sessionRecordingService.getRecordingForPlayback(
                        sessionId,
                        recordingId,
                        currentUserUtil.getCurrentUserId()
                );

        StreamingResponseBody stream = outputStream ->
                driveStorageService.downloadRecording(
                        recording.googleDriveFileId(),
                        outputStream
                );

        ResponseEntity.BodyBuilder response = ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(recording.mimeType())
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(recording.fileName())
                                .build()
                                .toString()
                )
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Content-Type-Options", "nosniff");

        if (recording.fileSizeBytes() != null
                && recording.fileSizeBytes() > 0) {
            response.contentLength(recording.fileSizeBytes());
        }

        return response.body(stream);
    }

}