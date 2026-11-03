package com.barterlink.sessions.controller;

import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import com.barterlink.sessions.dto.request.RescheduleSessionRequestDto;
import com.barterlink.sessions.dto.request.ScheduleSessionRequestDto;
import com.barterlink.sessions.dto.response.SessionResponseDto;
import com.barterlink.sessions.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.barterlink.sessions.dto.request.SessionModeSelectionRequestDto;
import com.barterlink.sessions.dto.request.SessionRecordingConsentRequestDto;
import com.barterlink.sessions.dto.response.SessionRecordingConsentResponseDto;
import com.barterlink.sessions.dto.response.SessionRecordingReadinessResponseDto;
import com.barterlink.sessions.dto.request.OpenDisputeRequestDto;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping("/{sessionId}")
    public ApiResponse<SessionResponseDto> getSession(
            @PathVariable UUID sessionId) {

        return ApiResponse.success(
                sessionService.getSession(sessionId));
    }

    @GetMapping("/requester")
    public ApiResponse<List<SessionResponseDto>> getRequesterSessions() {

        return ApiResponse.success(
                sessionService.getRequesterSessions(
                        currentUserUtil.getCurrentUserId()));
    }

    @GetMapping("/provider")
    public ApiResponse<List<SessionResponseDto>> getProviderSessions() {

        return ApiResponse.success(
                sessionService.getProviderSessions(
                        currentUserUtil.getCurrentUserId()));
    }

    @PutMapping("/{sessionId}/mode")
    public ApiResponse<SessionResponseDto> selectSessionMode(
            @PathVariable UUID sessionId,
            @RequestBody SessionModeSelectionRequestDto request) {

        return ApiResponse.success(
                "Session mode selection saved",
                sessionService.selectSessionMode(
                        sessionId,
                        currentUserUtil.getCurrentUserId(),
                        request));
    }

    @PostMapping("/{sessionId}/recording-consent")
    public ApiResponse<SessionRecordingConsentResponseDto> submitRecordingConsent(
            @PathVariable UUID sessionId,
            @RequestBody SessionRecordingConsentRequestDto request) {

        return ApiResponse.success(
                "Recording consent submitted",
                sessionService.submitRecordingConsent(
                        sessionId,
                        currentUserUtil.getCurrentUserId(),
                        request));
    }

    @GetMapping("/{sessionId}/recording-consent")
    public ApiResponse<SessionRecordingConsentResponseDto> getRecordingConsentStatus(
            @PathVariable UUID sessionId) {

        return ApiResponse.success(
                sessionService.getRecordingConsentStatus(
                        sessionId,
                        currentUserUtil.getCurrentUserId()));
    }

    @PostMapping("/{sessionId}/recording-readiness")
    public ApiResponse<SessionRecordingReadinessResponseDto> reportRecorderReady(
            @PathVariable UUID sessionId) {

        return ApiResponse.success(
                "Recorder preparation reported",
                sessionService.reportRecorderReady(
                        sessionId,
                        currentUserUtil.getCurrentUserId()));
    }


    @GetMapping("/{sessionId}/recording-readiness")
    public ApiResponse<SessionRecordingReadinessResponseDto> getRecordingReadiness(
            @PathVariable UUID sessionId) {

        return ApiResponse.success(
                sessionService.getRecordingReadiness(
                        sessionId,
                        currentUserUtil.getCurrentUserId()));
    }

    @PutMapping("/{sessionId}/start")
    public ApiResponse<SessionResponseDto> startSession(
            @PathVariable UUID sessionId) {

        return ApiResponse.success(
                "Session started",
                sessionService.startSession(sessionId, currentUserUtil.getCurrentUserId()));
    }

    @PutMapping("/{sessionId}/schedule")
    public ApiResponse<SessionResponseDto> scheduleSession(
            @PathVariable UUID sessionId,
            @RequestBody ScheduleSessionRequestDto request) {
        SessionResponseDto result = sessionService.scheduleSession(
                sessionId,
                currentUserUtil.getCurrentUserId(),
                request
        );

        return ApiResponse.success(
                "Session scheduled",
                result
        );
    }

    @PutMapping("/{sessionId}/accept-schedule")
    public ApiResponse<SessionResponseDto> acceptSchedule(
            @PathVariable UUID sessionId) {
        SessionResponseDto result = sessionService.acceptSchedule(
                sessionId,
                currentUserUtil.getCurrentUserId()
        );

        return ApiResponse.success(
                "Schedule accepted",
                result
        );
    }

    @PutMapping("/{sessionId}/reschedule")
    public ApiResponse<SessionResponseDto> rescheduleSession(
            @PathVariable UUID sessionId,
            @RequestBody RescheduleSessionRequestDto request) {

        return ApiResponse.success(
                "Session rescheduled",
                sessionService.rescheduleSession(
                        sessionId,
                        currentUserUtil.getCurrentUserId(),
                        request));
    }

    @PutMapping("/{sessionId}/provider-confirm")
    public ApiResponse<SessionResponseDto> providerConfirm(
            @PathVariable UUID sessionId) {
        SessionResponseDto result = sessionService.markWorkCompleted(
                sessionId,
                currentUserUtil.getCurrentUserId()
        );

        return ApiResponse.success(
                "Work marked as completed",
                result
        );
    }

    @PutMapping("/{sessionId}/requester-confirm")
    public ApiResponse<SessionResponseDto> requesterConfirm(
            @PathVariable UUID sessionId) {
        SessionResponseDto result = sessionService.confirmCompletion(
                sessionId,
                currentUserUtil.getCurrentUserId()
        );

        return ApiResponse.success(
                "Completion confirmed",
                result
        );
    }

    @PutMapping("/{sessionId}/dispute")
    public ApiResponse<SessionResponseDto> disputeSession(
            @PathVariable UUID sessionId,
            @Valid @RequestBody OpenDisputeRequestDto request) {

        return ApiResponse.success(
                "Session disputed",
                sessionService.disputeSession(
                        sessionId,
                        currentUserUtil.getCurrentUserId(),
                        request
                )
        );
    }

    @PutMapping("/{sessionId}/cancel")
    public ApiResponse<SessionResponseDto> cancelSession(
            @PathVariable UUID sessionId) {

        return ApiResponse.success(
                "Session cancelled",
                sessionService.cancelSession(
                        sessionId,
                        currentUserUtil.getCurrentUserId()));
    }
}