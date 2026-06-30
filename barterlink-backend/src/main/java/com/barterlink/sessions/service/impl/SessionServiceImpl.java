package com.barterlink.sessions.service.impl;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.agreements.repository.AgreementRepository;
import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.sessions.dto.response.SessionResponseDto;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.enums.SessionStatus;
import com.barterlink.sessions.repository.ServiceSessionRepository;
import com.barterlink.sessions.service.SessionService;
import com.barterlink.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final ServiceSessionRepository sessionRepository;
    private final WalletService walletService;
    private final AgreementRepository agreementRepository;
    private final CurrentUserUtil currentUserUtil;

    @Override
    public SessionResponseDto createSession(
            ServiceAgreement agreement) {

        sessionRepository.findByAgreement(agreement)
                .ifPresent(session -> {
                    throw new BusinessException(
                            "Session already exists");
                });

        ServiceSession session =
                ServiceSession.builder()
                        .agreement(agreement)
                        .status(SessionStatus.CREATED)
                        .build();

        return mapToResponse(
                sessionRepository.save(session));
    }

    @Override
    public SessionResponseDto getSession(
            UUID sessionId) {

        ServiceSession session =
                getSessionEntity(sessionId);

        validateParticipant(session);

        return mapToResponse(session);
    }

    @Override
    public List<SessionResponseDto> getRequesterSessions(
            UUID requesterId) {

        return sessionRepository
                .findByAgreementRequesterId(requesterId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<SessionResponseDto> getProviderSessions(
            UUID providerId) {

        return sessionRepository
                .findByAgreementProviderId(providerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public SessionResponseDto startSession(
            UUID sessionId) {

        ServiceSession session =
                getSessionEntity(sessionId);

        validateProvider(session);

        if (session.getStatus() != SessionStatus.CREATED) {
            throw new BusinessException(
                    "Session cannot be started");
        }

        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setStartedAt(Instant.now());

        return mapToResponse(
                sessionRepository.save(session));
    }

    @Override
    public SessionResponseDto providerConfirmCompletion(
            UUID sessionId) {

        ServiceSession session =
                getSessionEntity(sessionId);

        validateProvider(session);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException(
                    "Session is not in progress");
        }

        session.setProviderConfirmed(true);
        session.setProviderConfirmedAt(Instant.now());
        session.setStatus(SessionStatus.PROVIDER_CONFIRMED);

        return mapToResponse(
                sessionRepository.save(session));
    }

    @Transactional
    @Override
    public SessionResponseDto requesterConfirmCompletion(
            UUID sessionId) {

        ServiceSession session =
                getSessionEntity(sessionId);

        validateRequester(session);

        if (session.getStatus()
                != SessionStatus.PROVIDER_CONFIRMED) {

            throw new BusinessException(
                    "Provider has not confirmed yet");
        }

        session.setRequesterConfirmed(true);
        session.setRequesterConfirmedAt(Instant.now());

        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());

        ServiceAgreement agreement =
                session.getAgreement();

        if (agreement.getStatus() != AgreementStatus.ACTIVE) {
            throw new BusinessException(
                    "Agreement is not active");
        }

        walletService.transferLockedTokens(
                agreement.getRequester().getId(),
                agreement.getProvider().getId(),
                agreement.getEscrowLockedTokens(),
                "Session completed"
        );

        agreement.setStatus(
                AgreementStatus.COMPLETED);

        agreement.setCompletedAt(
                Instant.now());


        agreementRepository.save(agreement);

        return mapToResponse(
                sessionRepository.save(session));
    }

    @Override
    @Transactional
    public SessionResponseDto disputeSession(
            UUID sessionId) {

        ServiceSession session =
                getSessionEntity(sessionId);

        validateParticipant(session);

        if (session.getStatus() == SessionStatus.COMPLETED
                || session.getStatus() == SessionStatus.CANCELLED) {

            throw new BusinessException(
                    "Session cannot be disputed");
        }

        session.setStatus(SessionStatus.DISPUTED);

        ServiceAgreement agreement =
                session.getAgreement();

        agreement.setStatus(
                AgreementStatus.DISPUTED);

        agreementRepository.save(agreement);

        return mapToResponse(
                sessionRepository.save(session));
    }

    @Override
    @Transactional
    public SessionResponseDto cancelSession(
            UUID sessionId) {

        ServiceSession session =
                getSessionEntity(sessionId);

        validateParticipant(session);

        if (session.getStatus() == SessionStatus.COMPLETED
                || session.getStatus() == SessionStatus.CANCELLED) {

            throw new BusinessException(
                    "Session cannot be cancelled");
        }

        ServiceAgreement agreement =
                session.getAgreement();

        walletService.refundTokens(
                agreement.getRequester().getId(),
                agreement.getEscrowLockedTokens(),
                "Session cancelled"
        );

        session.setStatus(
                SessionStatus.CANCELLED);

        agreement.setStatus(
                AgreementStatus.CANCELLED);

        agreementRepository.save(agreement);

        return mapToResponse(
                sessionRepository.save(session));
    }

    private ServiceSession getSessionEntity(
            UUID sessionId) {

        return sessionRepository.findById(sessionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Session not found"));
    }


    private void validateProvider(
            ServiceSession session) {

        UUID currentUserId =
                currentUserUtil.getCurrentUserId();

        UUID providerId =
                session.getAgreement()
                        .getProvider()
                        .getId();

        if (!providerId.equals(currentUserId)) {

            throw new BusinessException(
                    "Only provider can perform this action");
        }
    }

    private void validateRequester(
            ServiceSession session) {

        UUID currentUserId =
                currentUserUtil.getCurrentUserId();

        UUID requesterId =
                session.getAgreement()
                        .getRequester()
                        .getId();

        if (!requesterId.equals(currentUserId)) {

            throw new BusinessException(
                    "Only requester can perform this action");
        }
    }



    private void validateParticipant(
            ServiceSession session) {

        UUID currentUserId =
                currentUserUtil.getCurrentUserId();

        UUID requesterId =
                session.getAgreement()
                        .getRequester()
                        .getId();

        UUID providerId =
                session.getAgreement()
                        .getProvider()
                        .getId();

        if (!requesterId.equals(currentUserId)
                && !providerId.equals(currentUserId)) {

            throw new BusinessException(
                    "Access denied");
        }
    }



    private SessionResponseDto mapToResponse(
            ServiceSession session) {

        SessionResponseDto dto =
                new SessionResponseDto();

        dto.setSessionId(session.getId());

        dto.setAgreementId(
                session.getAgreement().getId());

        dto.setRequesterId(
                session.getAgreement()
                        .getRequester()
                        .getId());

        dto.setRequesterName(
                session.getAgreement()
                        .getRequester()
                        .getName());

        dto.setProviderId(
                session.getAgreement()
                        .getProvider()
                        .getId());

        dto.setProviderName(
                session.getAgreement()
                        .getProvider()
                        .getName());

        dto.setStatus(session.getStatus());

        dto.setProviderConfirmed(
                session.getProviderConfirmed());

        dto.setRequesterConfirmed(
                session.getRequesterConfirmed());

        dto.setStartedAt(
                session.getStartedAt());

        dto.setProviderConfirmedAt(
                session.getProviderConfirmedAt());

        dto.setRequesterConfirmedAt(
                session.getRequesterConfirmedAt());

        dto.setCompletedAt(
                session.getCompletedAt());

        dto.setCreatedAt(
                session.getCreatedAt());

        return dto;
    }
}
