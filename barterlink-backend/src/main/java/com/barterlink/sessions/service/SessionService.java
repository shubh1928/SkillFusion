package com.barterlink.sessions.service;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.sessions.dto.response.SessionResponseDto;

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

    SessionResponseDto startSession(
            UUID sessionId);

    SessionResponseDto providerConfirmCompletion(
            UUID sessionId);

    SessionResponseDto requesterConfirmCompletion(
            UUID sessionId);

    SessionResponseDto disputeSession(
            UUID sessionId);

    SessionResponseDto cancelSession(
            UUID sessionId);
}
