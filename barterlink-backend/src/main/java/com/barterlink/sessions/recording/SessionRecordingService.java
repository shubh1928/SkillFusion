package com.barterlink.sessions.recording;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.sessions.dto.response.SessionRecordingResponseDto;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.entity.SessionRecording;
import com.barterlink.sessions.entity.SessionRecordingConsent;
import com.barterlink.sessions.enums.SessionMode;
import com.barterlink.sessions.enums.SessionRecordingStatus;
import com.barterlink.sessions.repository.ServiceSessionRepository;
import com.barterlink.sessions.repository.SessionRecordingConsentRepository;
import com.barterlink.sessions.repository.SessionRecordingRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.stream.Collectors;
import com.google.api.services.drive.model.File;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionRecordingService {

    private static final String RECORDING_POLICY_VERSION =
            "SESSION_RECORDING_POLICY_V1";

    private static final List<String> SUPPORTED_VIDEO_TYPES =
            List.of("video/webm", "video/mp4");

    private final ServiceSessionRepository sessionRepository;
    private final SessionRecordingRepository recordingRepository;
    private final SessionRecordingConsentRepository consentRepository;
    private final UserRepository userRepository;
    private final GoogleDriveStorageService driveStorageService;
    private final TransactionTemplate transactionTemplate;

    public SessionRecordingService(
            ServiceSessionRepository sessionRepository,
            SessionRecordingRepository recordingRepository,
            SessionRecordingConsentRepository consentRepository,
            UserRepository userRepository,
            GoogleDriveStorageService driveStorageService,
            PlatformTransactionManager transactionManager
    ) {
        this.sessionRepository = sessionRepository;
        this.recordingRepository = recordingRepository;
        this.consentRepository = consentRepository;
        this.userRepository = userRepository;
        this.driveStorageService = driveStorageService;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    public SessionRecordingResponseDto uploadRecording(
            UUID sessionId,
            UUID userId,
            MultipartFile file,
            Long durationSeconds
    ) throws IOException {

        validateUpload(file, durationSeconds);

        String mimeType = normalizeMimeType(file.getContentType());

        long fileSizeBytes = file.getSize();

        String extension = "video/mp4".equals(mimeType)
                ? ".mp4"
                : ".webm";

        // Generate the filename on the server instead of trusting
        // the filename supplied by the client.
        String fileName =
                "skillfusion-session-"
                        + sessionId
                        + "-"
                        + UUID.randomUUID()
                        + extension;

        // Transaction 1:
        // Validate the participant, session and both consents.
        // Persist the upload record before calling Google Drive.
        UUID recordingId = transactionTemplate.execute(status -> {

            ServiceSession session = sessionRepository
                    .findById(sessionId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Session not found"));

            UUID requesterId = session.getAgreement()
                    .getRequester()
                    .getId();

            UUID providerId = session.getAgreement()
                    .getProvider()
                    .getId();

            if (!requesterId.equals(userId)
                    && !providerId.equals(userId)) {
                throw new BusinessException(
                        "You are not part of this session");
            }

            if (session.getAgreedSessionMode()
                    != SessionMode.LIVE_VIDEO_SESSION
                    || session.getModeAgreedAt() == null) {
                throw new BusinessException(
                        "Recording is available only for an agreed live-video session");
            }

            // A recording must not be uploaded for a session
            // that never actually started.
            if (session.getStartedAt() == null) {
                throw new BusinessException(
                        "The session must have started before its recording can be uploaded");
            }

            Optional<SessionRecordingConsent> requesterConsent =
                    findCurrentConsent(
                            sessionId,
                            requesterId,
                            session.getModeAgreedAt());

            Optional<SessionRecordingConsent> providerConsent =
                    findCurrentConsent(
                            sessionId,
                            providerId,
                            session.getModeAgreedAt());

            if (requesterConsent.isEmpty()
                    || providerConsent.isEmpty()
                    || !requesterConsent.get().isRecordingConsent()
                    || !providerConsent.get().isRecordingConsent()) {
                throw new BusinessException(
                        "Both participants must have valid recording consent");
            }

            User createdBy = userRepository.findById(userId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "User not found"));

            boolean educationalReusePermitted =
                    requesterConsent.get().isEducationalReuseConsent()
                            && providerConsent.get()
                                    .isEducationalReuseConsent();

            SessionRecording recording = SessionRecording.builder()
                    .session(session)
                    .createdBy(createdBy)
                    .fileName(fileName)
                    .mimeType(mimeType)
                    .status(SessionRecordingStatus.UPLOADING)
                    .fileSizeBytes(fileSizeBytes)
                    .durationSeconds(durationSeconds)
                    .consentPolicyVersion(RECORDING_POLICY_VERSION)
                    .modeAgreedAt(session.getModeAgreedAt())
                    .educationalReusePermitted(
                            educationalReusePermitted)
                    .uploadStartedAt(Instant.now())
                    .build();

            return recordingRepository.saveAndFlush(recording)
                    .getId();
        });

        if (recordingId == null) {
            throw new IllegalStateException(
                    "The recording upload could not be initialized");
        }

        File uploadedDriveFile = null;

        try {
            // Hash in a separate pass. This avoids computing the
            // digest over bytes that the resumable uploader may retry.
            String sha256 = calculateSha256(file);

            uploadedDriveFile = driveStorageService.uploadRecording(
                    file.getInputStream(),
                    fileSizeBytes,
                    fileName,
                    mimeType
            );

            final File completedDriveFile = uploadedDriveFile;

            // Transaction 2:
            // Mark READY only after Google Drive returns a file ID.
            SessionRecordingResponseDto response =
                    transactionTemplate.execute(status -> {

                        SessionRecording recording =
                                recordingRepository.findById(recordingId)
                                        .orElseThrow(() ->
                                                new ResourceNotFoundException(
                                                        "Recording metadata not found"));

                        recording.setGoogleDriveFileId(
                                completedDriveFile.getId());

                        recording.setFileSizeBytes(
                                completedDriveFile.getSize() != null
                                        ? completedDriveFile.getSize()
                                        : fileSizeBytes);

                        recording.setSha256(sha256);
                        recording.setUploadedAt(Instant.now());
                        recording.setStatus(
                                SessionRecordingStatus.READY);
                        recording.setFailureReason(null);

                        SessionRecording saved =
                                recordingRepository.save(recording);

                        return SessionRecordingResponseDto.from(saved);
                    });

            if (response == null) {
                throw new IllegalStateException(
                        "Recording metadata could not be finalized");
            }

            return response;

        } catch (IOException | RuntimeException exception) {

            // Persist FAILED separately so a failed Drive upload
            // does not roll back the initial metadata record.
            markUploadFailed(
                    recordingId,
                    exception,
                    uploadedDriveFile != null
                            ? uploadedDriveFile.getId()
                            : null
            );

            throw exception;
        }
    }

    private Optional<SessionRecordingConsent> findCurrentConsent(
            UUID sessionId,
            UUID userId,
            Instant modeAgreedAt
    ) {
        return consentRepository
                .findTopBySession_IdAndUser_IdAndModeAgreedAtOrderByDecidedAtDesc(
                        sessionId,
                        userId,
                        modeAgreedAt)
                .filter(consent ->
                        RECORDING_POLICY_VERSION.equals(
                                consent.getPolicyVersion()));
    }

    private void validateUpload(
            MultipartFile file,
            Long durationSeconds
    ) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(
                    "A non-empty recording file is required");
        }

        String mimeType = normalizeMimeType(file.getContentType());

        if (!SUPPORTED_VIDEO_TYPES.contains(mimeType)) {
            throw new BusinessException(
                    "Only WebM and MP4 recordings are supported");
        }

        if (durationSeconds != null && durationSeconds < 0) {
            throw new BusinessException(
                    "Recording duration cannot be negative");
        }
    }

    private String normalizeMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) {
            throw new BusinessException(
                    "Recording MIME type is required");
        }

        int parameterIndex = mimeType.indexOf(';');

        String baseMimeType = parameterIndex >= 0
                ? mimeType.substring(0, parameterIndex)
                : mimeType;

        return baseMimeType.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String calculateSha256(
            MultipartFile file
    ) throws IOException {

        final MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available in this Java runtime",
                    exception);
        }

        try (InputStream input = file.getInputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        return HexFormat.of().formatHex(digest.digest());
    }

    private void markUploadFailed(
            UUID recordingId,
            Exception originalException,
            String uploadedDriveFileId
    ) {
        try {
            transactionTemplate.executeWithoutResult(status -> {

                recordingRepository.findById(recordingId)
                        .ifPresent(recording -> {

                            recording.setStatus(
                                    SessionRecordingStatus.FAILED);

                            String message =
                                    originalException.getClass()
                                            .getSimpleName();

                            recording.setFailureReason(
                                    message.length() > 1000
                                            ? message.substring(0, 1000)
                                            : message);

                            // Preserve the ID if Drive succeeded but
                            // the metadata finalization subsequently failed.
                            if (StringUtils.hasText(
                                    uploadedDriveFileId)) {
                                recording.setGoogleDriveFileId(
                                        uploadedDriveFileId);
                            }

                            recordingRepository.save(recording);
                        });
            });

        } catch (RuntimeException persistenceException) {
            originalException.addSuppressed(persistenceException);
        }
    }

    @Transactional(readOnly = true)
    public List<SessionRecordingResponseDto> getSessionRecordings(
            UUID sessionId,
            UUID userId
    ) {
        ServiceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Session not found"));

        UUID requesterId = session.getAgreement()
                .getRequester()
                .getId();

        UUID providerId = session.getAgreement()
                .getProvider()
                .getId();

        if (!requesterId.equals(userId)
                && !providerId.equals(userId)) {
            throw new BusinessException(
                    "You are not part of this session");
        }

        return recordingRepository
                .findBySession_IdOrderByCreatedAtDesc(sessionId)
                .stream()
                .map(SessionRecordingResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SessionRecordingPlayback getRecordingForPlayback(
            UUID sessionId,
            UUID recordingId,
            UUID userId
    ) {
        ServiceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Session not found"));

        UUID requesterId = session.getAgreement()
                .getRequester()
                .getId();

        UUID providerId = session.getAgreement()
                .getProvider()
                .getId();

        if (!requesterId.equals(userId)
                && !providerId.equals(userId)) {
            throw new BusinessException(
                    "You are not part of this session");
        }

        SessionRecording recording =
                recordingRepository.findByIdAndSession_Id(
                        recordingId,
                        sessionId
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Recording not found"));

        if (recording.getStatus() != SessionRecordingStatus.READY) {
            throw new BusinessException(
                    "This recording is not ready for playback");
        }

        if (!StringUtils.hasText(recording.getGoogleDriveFileId())) {
            throw new BusinessException(
                    "The recording's storage reference is unavailable");
        }

        return new SessionRecordingPlayback(
                recording.getGoogleDriveFileId(),
                recording.getFileName(),
                recording.getMimeType(),
                recording.getFileSizeBytes()
        );
    }

}