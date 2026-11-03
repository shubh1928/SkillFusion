package com.barterlink.sessions.service;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.sessions.dto.request.RescheduleSessionRequestDto;
import com.barterlink.sessions.dto.request.ScheduleSessionRequestDto;
import com.barterlink.sessions.dto.response.SessionResponseDto;
import com.barterlink.sessions.dto.request.SessionModeSelectionRequestDto;
import com.barterlink.sessions.dto.request.SessionRecordingConsentRequestDto;
import com.barterlink.sessions.dto.response.SessionRecordingConsentResponseDto;
import com.barterlink.sessions.dto.response.SessionRecordingReadinessResponseDto;
import com.barterlink.sessions.dto.request.OpenDisputeRequestDto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface SessionService {

    SessionResponseDto createSession(
            ServiceAgreement agreement);

    SessionResponseDto getSession(
            UUID sessionId);

    List<SessionResponseDto> getRequesterSessions(
            UUID requesterId);

    List<SessionResponseDto> getProviderSessions(
            UUID providerId);

    SessionResponseDto selectSessionMode(
            UUID sessionId,
            UUID userId,
            SessionModeSelectionRequestDto request);

    SessionRecordingConsentResponseDto submitRecordingConsent(
            UUID sessionId,
            UUID userId,
            SessionRecordingConsentRequestDto request);

    SessionRecordingConsentResponseDto getRecordingConsentStatus(
            UUID sessionId,
            UUID userId);

    SessionRecordingReadinessResponseDto reportRecorderReady(
            UUID sessionId,
            UUID userId);

    SessionRecordingReadinessResponseDto getRecordingReadiness(
            UUID sessionId,
            UUID userId);

    /**
     * Validates whether an authenticated session participant is allowed
     * to use live-media signaling.
     *
     * Requires the agreed live-video mode, current recording consent from
     * both participants, recorder readiness from both participants, and
     * a session that has actually started.
     */
    void assertLiveMediaReady(
            UUID sessionId,
            UUID userId);

    SessionResponseDto startSession(
            UUID sessionId,
            UUID userId);

    SessionResponseDto markWorkCompleted(
            UUID sessionId,
            UUID userId);

    SessionResponseDto confirmCompletion(
            UUID sessionId,
            UUID userId);

    /**
     * Completes a Milestone Release session only when every milestone
     * in the active version has been released.
     *
     * @return true when the agreement and session are completed
     */
    boolean completeMilestoneSessionIfFinal(UUID agreementId);

    SessionResponseDto disputeSession(
            UUID sessionId,
            UUID userId,
            OpenDisputeRequestDto request
    );

    SessionResponseDto cancelSession(
            UUID sessionId,
            UUID userId);

    SessionResponseDto scheduleSession(
            UUID sessionId,
            UUID userId,
            ScheduleSessionRequestDto request);

    SessionResponseDto rescheduleSession(
            UUID sessionId,
            UUID userId,
            RescheduleSessionRequestDto request);

    SessionResponseDto acceptSchedule(
            UUID sessionId,
            UUID userId);

}
