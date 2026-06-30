package com.barterlink.sessions.controller;

import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import com.barterlink.sessions.dto.response.SessionResponseDto;
import com.barterlink.sessions.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @PutMapping("/{sessionId}/start")
    public ApiResponse<SessionResponseDto> startSession(
            @PathVariable UUID sessionId) {

        return ApiResponse.success(
                "Session started",
                sessionService.startSession(sessionId));
    }

    @PutMapping("/{sessionId}/provider-confirm")
    public ApiResponse<SessionResponseDto> providerConfirm(
            @PathVariable UUID sessionId) {

        return ApiResponse.success(
                "Provider confirmation received",
                sessionService.providerConfirmCompletion(
                        sessionId));
    }

    @PutMapping("/{sessionId}/requester-confirm")
    public ApiResponse<SessionResponseDto> requesterConfirm(
            @PathVariable UUID sessionId) {

        return ApiResponse.success(
                "Requester confirmation received",
                sessionService.requesterConfirmCompletion(
                        sessionId));
    }

    @PutMapping("/{sessionId}/dispute")
    public ApiResponse<SessionResponseDto> disputeSession(
            @PathVariable UUID sessionId) {

        return ApiResponse.success(
                "Session disputed",
                sessionService.disputeSession(
                        sessionId));
    }

    @PutMapping("/{sessionId}/cancel")
    public ApiResponse<SessionResponseDto> cancelSession(
            @PathVariable UUID sessionId) {

        return ApiResponse.success(
                "Session cancelled",
                sessionService.cancelSession(
                        sessionId));
    }
}