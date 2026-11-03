
package com.barterlink.sessions.service.impl;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.agreements.repository.AgreementRepository;
import com.barterlink.ai.service.TrustAssessmentService;
import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.collaboration.service.CollaborationRoomService;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.negotiation.service.NegotiationTimelineService;
import com.barterlink.sessions.dto.request.RescheduleSessionRequestDto;
import com.barterlink.sessions.dto.request.ScheduleSessionRequestDto;
import com.barterlink.sessions.dto.response.SessionResponseDto;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.enums.SessionStatus;
import com.barterlink.sessions.repository.ServiceSessionRepository;
import com.barterlink.sessions.service.SessionService;
import com.barterlink.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.barterlink.notifications.enums.NotificationType;
import com.barterlink.notifications.service.NotificationService;
import com.barterlink.websocket.service.SessionRealtimeService;
import com.barterlink.blockchain.escrow.BlockchainEscrowTransactionService;
import com.barterlink.blockchain.escrow.BlockchainEscrowService;
import com.barterlink.wallet.dto.WalletSummaryDto;
import com.barterlink.agreements.enums.AgreementReleaseMode;
import com.barterlink.agreements.entity.AgreementVersionMilestone;
import com.barterlink.agreements.repository.AgreementVersionMilestoneRepository;
import com.barterlink.agreements.enums.AgreementVersionStatus;
import com.barterlink.sessions.dto.request.SessionModeSelectionRequestDto;
import com.barterlink.sessions.enums.SessionMode;
import com.barterlink.sessions.dto.request.SessionRecordingConsentRequestDto;
import com.barterlink.sessions.dto.response.SessionRecordingConsentResponseDto;
import com.barterlink.sessions.entity.SessionRecordingConsent;
import com.barterlink.sessions.repository.SessionRecordingConsentRepository;
import com.barterlink.sessions.dto.response.SessionRecordingReadinessResponseDto;
import com.barterlink.sessions.dto.request.OpenDisputeRequestDto;
import com.barterlink.disputes.entity.DisputeCase;
import com.barterlink.disputes.enums.DisputeCaseStatus;
import com.barterlink.disputes.repository.DisputeCaseRepository;
import com.barterlink.disputes.service.DisputePanelAssignmentService;
import com.barterlink.disputes.entity.DisputePanelAssignment;
import com.barterlink.disputes.service.DisputeEvidenceService;
import com.barterlink.rewards.service.RewardClaimQueueService;
import com.barterlink.rewards.service.ReferralAttributionService;
import com.barterlink.auth.services.UserActivityService;
import com.barterlink.rewards.enums.RewardQualifyingActivityType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private static final String RECORDING_POLICY_VERSION =
            "SESSION_RECORDING_POLICY_V1";

    private final ServiceSessionRepository sessionRepository;
    private final SessionRecordingConsentRepository sessionRecordingConsentRepository;
    private final WalletService walletService;
    private final BlockchainEscrowTransactionService blockchainEscrowTransactionService;
    private final BlockchainEscrowService blockchainEscrowService;
    private final AgreementRepository agreementRepository;
    private final AgreementVersionMilestoneRepository agreementVersionMilestoneRepository;
    private final CurrentUserUtil currentUserUtil;
    private final UserRepository userRepository;
    private final NegotiationTimelineService negotiationTimelineService;
    private final NotificationService notificationService;
    private final SessionRealtimeService sessionRealtimeService;
    private final CollaborationRoomService collaborationRoomService;
    private final TrustAssessmentService trustAssessmentService;
    private final DisputeCaseRepository disputeCaseRepository;
    private final DisputePanelAssignmentService disputePanelAssignmentService;
    private final DisputeEvidenceService disputeEvidenceService;
    private final RewardClaimQueueService rewardClaimQueueService;
    private final ReferralAttributionService referralAttributionService;
    private final UserActivityService userActivityService;

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

        ServiceSession savedSession =
                sessionRepository.save(session);

        collaborationRoomService.createRoom(
                savedSession.getId());

        return mapToResponse(savedSession);
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
    @Transactional
    public SessionResponseDto selectSessionMode(
            UUID sessionId,
            UUID userId,
            SessionModeSelectionRequestDto request) {

        if (request == null || request.getSelectedMode() == null) {
            throw new BusinessException(
                    "Session mode selection is required");
        }

        ServiceSession session =
                getSessionEntity(sessionId);

        if (session.getStatus() != SessionStatus.CREATED
                && session.getStatus() != SessionStatus.SCHEDULED) {

            throw new BusinessException(
                    "Session mode can only be selected before the session starts");
        }

        UUID requesterId =
                session.getAgreement()
                        .getRequester()
                        .getId();

        UUID providerId =
                session.getAgreement()
                        .getProvider()
                        .getId();

        if (!requesterId.equals(userId)
                && !providerId.equals(userId)) {

            throw new BusinessException(
                    "You are not part of this session");
        }

        if (Boolean.TRUE.equals(session.getRequesterStarted())
                || Boolean.TRUE.equals(session.getProviderStarted())) {

            throw new BusinessException(
                    "Session mode cannot be changed after a participant has started the session");
        }

        Instant now = Instant.now();

        if (requesterId.equals(userId)) {

            session.setRequesterSelectedMode(
                    request.getSelectedMode());

            session.setRequesterModeSelectedAt(now);

        } else {

            session.setProviderSelectedMode(
                    request.getSelectedMode());

            session.setProviderModeSelectedAt(now);
        }

        SessionMode requesterMode =
                session.getRequesterSelectedMode();

        SessionMode providerMode =
                session.getProviderSelectedMode();

        if (requesterMode != null
                && requesterMode == providerMode) {

            if (session.getAgreedSessionMode() != requesterMode
                    || session.getModeAgreedAt() == null) {

                session.setRequesterRecorderReadyAt(null);
                session.setProviderRecorderReadyAt(null);

                session.setRequesterLivePreparationRequestedAt(null);
                session.setProviderLivePreparationRequestedAt(null);

                session.setAgreedSessionMode(requesterMode);
                session.setModeAgreedAt(now);
            }

        } else {

            session.setAgreedSessionMode(null);
            session.setModeAgreedAt(null);

            session.setRequesterRecorderReadyAt(null);
            session.setProviderRecorderReadyAt(null);

            session.setRequesterLivePreparationRequestedAt(null);
            session.setProviderLivePreparationRequestedAt(null);
        }

        ServiceSession savedSession =
                sessionRepository.save(session);

        return mapToResponse(savedSession);
    }

    @Override
    @Transactional
    public SessionRecordingConsentResponseDto submitRecordingConsent(
            UUID sessionId,
            UUID userId,
            SessionRecordingConsentRequestDto request) {

        if (request == null
                || request.getRecordingConsent() == null
                || request.getEducationalReuseConsent() == null) {

            throw new BusinessException(
                    "Both recording and educational-reuse decisions must be explicitly provided");
        }

        ServiceSession session =
                getSessionEntity(sessionId);

        UUID requesterId =
                session.getAgreement()
                        .getRequester()
                        .getId();

        UUID providerId =
                session.getAgreement()
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
                    "Both participants must agree to live video before submitting recording consent");
        }

        if (session.getStatus() != SessionStatus.CREATED
                && session.getStatus() != SessionStatus.SCHEDULED) {

            throw new BusinessException(
                    "Recording consent must be completed before the session starts");
        }

        if (Boolean.TRUE.equals(session.getRequesterStarted())
                || Boolean.TRUE.equals(session.getProviderStarted())) {

            throw new BusinessException(
                    "Recording consent must be completed before session start confirmations");
        }

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        SessionRecordingConsent consent =
                SessionRecordingConsent.builder()
                        .session(session)
                        .user(user)
                        .recordingConsent(
                                request.getRecordingConsent())
                        .educationalReuseConsent(
                                request.getEducationalReuseConsent())
                        .policyVersion(RECORDING_POLICY_VERSION)
                        .modeAgreedAt(session.getModeAgreedAt())
                        .decidedAt(Instant.now())
                        .build();

        sessionRecordingConsentRepository.save(consent);

        return buildRecordingConsentResponse(session);
    }

    @Override
    @Transactional(readOnly = true)
    public SessionRecordingConsentResponseDto getRecordingConsentStatus(
            UUID sessionId,
            UUID userId) {

        ServiceSession session =
                getSessionEntity(sessionId);

        UUID requesterId =
                session.getAgreement()
                        .getRequester()
                        .getId();

        UUID providerId =
                session.getAgreement()
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
                    "Recording consent status is available only after both participants agree to live video");
        }

        return buildRecordingConsentResponse(session);
    }


    @Override
    @Transactional
    public SessionRecordingReadinessResponseDto reportRecorderReady(
            UUID sessionId,
            UUID userId) {

        ServiceSession session = getSessionEntity(sessionId);

        UUID requesterId = session.getAgreement()
                .getRequester()
                .getId();

        UUID providerId = session.getAgreement()
                .getProvider()
                .getId();

        // Only the two participants can report readiness.
        if (!requesterId.equals(userId)
                && !providerId.equals(userId)) {
            throw new BusinessException(
                    "You are not part of this session");
        }

        // Readiness reporting is only for the agreed live mode.
        if (session.getAgreedSessionMode()
                != SessionMode.LIVE_VIDEO_SESSION
                || session.getModeAgreedAt() == null) {
            throw new BusinessException(
                    "Both participants must agree to live video first");
        }

        // Recorder preparation must be reported before session start.
        if (session.getStatus() != SessionStatus.CREATED
                && session.getStatus() != SessionStatus.SCHEDULED) {
            throw new BusinessException(
                    "Recorder readiness must be reported before session start");
        }

        if (Boolean.TRUE.equals(session.getRequesterStarted())
                || Boolean.TRUE.equals(session.getProviderStarted())) {
            throw new BusinessException(
                    "Recorder readiness must be reported before either participant starts the session");
        }

        // Both participants must have explicitly consented to recording.
        SessionRecordingConsentResponseDto consentStatus =
                buildRecordingConsentResponse(session);

        if (!consentStatus.isBothParticipantsConsented()) {
            throw new BusinessException(
                    "Both participants must consent to recording before reporting recorder readiness");
        }

        // Record the first successful preparation report for this mode agreement.
        Instant now = Instant.now();

        if (requesterId.equals(userId)) {
            if (session.getRequesterRecorderReadyAt() == null) {
                session.setRequesterRecorderReadyAt(now);
            }
        } else {
            if (session.getProviderRecorderReadyAt() == null) {
                session.setProviderRecorderReadyAt(now);
            }
        }

        ServiceSession savedSession = sessionRepository.save(session);

        return buildRecordingReadinessResponse(
                savedSession,
                userId);
    }


    @Override
    @Transactional(readOnly = true)
    public SessionRecordingReadinessResponseDto getRecordingReadiness(
            UUID sessionId,
            UUID userId) {

        ServiceSession session = getSessionEntity(sessionId);

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
                    "Recording readiness is available only after both participants agree to live video");
        }

        return buildRecordingReadinessResponse(
                session,
                userId);
    }

    @Override
    @Transactional(readOnly = true)
    public void assertLiveMediaReady(
            UUID sessionId,
            UUID userId) {

        ServiceSession session = getSessionEntity(sessionId);

        UUID requesterId = session.getAgreement()
                .getRequester()
                .getId();

        UUID providerId = session.getAgreement()
                .getProvider()
                .getId();

        // Only the requester or provider may use live media.
        if (!requesterId.equals(userId)
                && !providerId.equals(userId)) {

            throw new BusinessException(
                    "You are not part of this session");
        }

        // The participants must have agreed to the current live-video mode.
        if (session.getAgreedSessionMode()
                != SessionMode.LIVE_VIDEO_SESSION
                || session.getModeAgreedAt() == null) {

            throw new BusinessException(
                    "Live media is allowed only for the currently agreed live-video session mode");
        }

        // Both participants must have started the session.
        if (session.getStatus() != SessionStatus.IN_PROGRESS
                || session.getStartedAt() == null
                || !Boolean.TRUE.equals(session.getRequesterStarted())
                || !Boolean.TRUE.equals(session.getProviderStarted())) {

            throw new BusinessException(
                    "The live session has not been started by both participants");
        }

        // Both participants must have valid recording consent.
        SessionRecordingConsentResponseDto consentStatus =
                buildRecordingConsentResponse(session);

        if (!consentStatus.isBothParticipantsConsented()) {

            throw new BusinessException(
                    "Both participants must have current recording consent before live media is allowed");
        }

        // Both participants must have reported recorder readiness.
        if (session.getRequesterRecorderReadyAt() == null
                || session.getProviderRecorderReadyAt() == null) {

            throw new BusinessException(
                    "Both participants must report recorder readiness before live media is allowed");
        }
    }


    private SessionRecordingReadinessResponseDto
    buildRecordingReadinessResponse(
            ServiceSession session,
            UUID userId) {

        UUID requesterId = session.getAgreement()
                .getRequester()
                .getId();

        UUID providerId = session.getAgreement()
                .getProvider()
                .getId();

        boolean requesterReady =
                session.getRequesterRecorderReadyAt() != null;

        boolean providerReady =
                session.getProviderRecorderReadyAt() != null;

        SessionRecordingConsentResponseDto consentStatus =
                buildRecordingConsentResponse(session);

        SessionRecordingReadinessResponseDto response =
                new SessionRecordingReadinessResponseDto();

        response.setSessionId(session.getId());

        response.setModeAgreedAt(session.getModeAgreedAt());

        response.setRequesterRecorderReadyAt(
                session.getRequesterRecorderReadyAt());

        response.setProviderRecorderReadyAt(
                session.getProviderRecorderReadyAt());

        response.setRequesterRecorderReady(requesterReady);

        response.setProviderRecorderReady(providerReady);

        response.setCurrentUserRecorderReady(
                requesterId.equals(userId)
                        ? requesterReady
                        : providerReady);

        response.setBothParticipantsConsented(
                consentStatus.isBothParticipantsConsented());

        response.setBothParticipantsRecorderReady(
                requesterReady
                        && providerReady
                        && consentStatus.isBothParticipantsConsented());

        return response;
    }


    private SessionRecordingConsentResponseDto
    buildRecordingConsentResponse(ServiceSession session) {

        UUID sessionId = session.getId();

        UUID requesterId =
                session.getAgreement()
                        .getRequester()
                        .getId();

        UUID providerId =
                session.getAgreement()
                        .getProvider()
                        .getId();

        Instant modeAgreedAt =
                session.getModeAgreedAt();

        SessionRecordingConsent requesterConsent =
                sessionRecordingConsentRepository
                        .findTopBySession_IdAndUser_IdAndModeAgreedAtOrderByDecidedAtDesc(
                                sessionId,
                                requesterId,
                                modeAgreedAt)
                        .filter(consent ->
                                RECORDING_POLICY_VERSION.equals(
                                        consent.getPolicyVersion()))
                        .orElse(null);

        SessionRecordingConsent providerConsent =
                sessionRecordingConsentRepository
                        .findTopBySession_IdAndUser_IdAndModeAgreedAtOrderByDecidedAtDesc(
                                sessionId,
                                providerId,
                                modeAgreedAt)
                        .filter(consent ->
                                RECORDING_POLICY_VERSION.equals(
                                        consent.getPolicyVersion()))
                        .orElse(null);

        SessionRecordingConsentResponseDto response =
                new SessionRecordingConsentResponseDto();

        response.setSessionId(sessionId);

        response.setPolicyVersion(
                RECORDING_POLICY_VERSION);

        response.setModeAgreedAt(
                modeAgreedAt);

        if (requesterConsent != null) {

            response.setRequesterRecordingConsent(
                    requesterConsent.isRecordingConsent());

            response.setRequesterEducationalReuseConsent(
                    requesterConsent.isEducationalReuseConsent());

            response.setRequesterConsentAt(
                    requesterConsent.getDecidedAt());
        }

        if (providerConsent != null) {

            response.setProviderRecordingConsent(
                    providerConsent.isRecordingConsent());

            response.setProviderEducationalReuseConsent(
                    providerConsent.isEducationalReuseConsent());

            response.setProviderConsentAt(
                    providerConsent.getDecidedAt());
        }

        boolean bothConsented =
                requesterConsent != null
                        && requesterConsent.isRecordingConsent()
                        && providerConsent != null
                        && providerConsent.isRecordingConsent();

        response.setBothParticipantsConsented(
                bothConsented);

        return response;
    }

    @Override
    @Transactional
    public SessionResponseDto startSession(
            UUID sessionId,
            UUID userId) {

        ServiceSession session =
                getSessionEntity(sessionId);

        if (session.getAgreement().getReleaseMode()
                == AgreementReleaseMode.MILESTONE_RELEASE
                && session.getAgreement().getActiveVersion() == null) {

            throw new BusinessException(
                    "Milestone Release cannot start until milestone 1 is funded and the agreement version is active");
        }

        if (session.getStatus() != SessionStatus.CREATED
                && session.getStatus() != SessionStatus.SCHEDULED) {

            throw new BusinessException(
                    "Session cannot be started");
        }

        UUID requesterId =
                session.getAgreement()
                        .getRequester()
                        .getId();

        UUID providerId =
                session.getAgreement()
                        .getProvider()
                        .getId();

        if (!requesterId.equals(userId)
                && !providerId.equals(userId)) {

            throw new BusinessException(
                    "You are not part of this session");
        }

        if (session.getAgreedSessionMode() == null) {
            throw new BusinessException(
                    "Both participants must agree on a session mode before starting");
        }

        if (session.getAgreedSessionMode()
                == SessionMode.LIVE_VIDEO_SESSION) {

            if (session.getModeAgreedAt() == null) {
                throw new BusinessException(
                        "The live-video session agreement is not finalized");
            }

            SessionRecordingConsentResponseDto consentStatus =
                    buildRecordingConsentResponse(session);

            if (!consentStatus.isBothParticipantsConsented()) {
                throw new BusinessException(
                        "Both participants must consent to recording before the live session can start");
            }

            if (session.getRequesterRecorderReadyAt() == null
                    || session.getProviderRecorderReadyAt() == null) {
                throw new BusinessException(
                        "Both participants must report recording readiness before the live session can start");
            }
        }

        /*
         * Record who clicked Start Session.
         */
        if (requesterId.equals(userId)) {

            session.setRequesterStarted(true);

        } else {

            session.setProviderStarted(true);
        }

        /*
         * Scheduled sessions must first
         * be accepted by the other participant.
         */
        if (session.getStatus() == SessionStatus.SCHEDULED
                && session.getScheduleAcceptedAt() == null) {

            throw new BusinessException(
                    "Schedule must be accepted before the session can start.");
        }

        boolean sessionJustStarted = false;

        /*
         * Session starts only after BOTH
         * participants have clicked Start.
         */
        if (Boolean.TRUE.equals(session.getRequesterStarted())
                && Boolean.TRUE.equals(session.getProviderStarted())) {

            session.setStatus(
                    SessionStatus.IN_PROGRESS);

            if (session.getStartedAt() == null) {

                session.setStartedAt(
                        Instant.now());

                sessionJustStarted = true;

                negotiationTimelineService.createSystemMessage(
                        session.getAgreement().getNegotiation(),
                        "Session started.");

                notificationService.createNotification(
                        session.getAgreement().getRequester().getId(),
                        NotificationType.SESSION,
                        "Session Started",
                        "The service session has started.");

                notificationService.createNotification(
                        session.getAgreement().getProvider().getId(),
                        NotificationType.SESSION,
                        "Session Started",
                        "The service session has started.");
            }
        }

        ServiceSession savedSession =
                sessionRepository.save(session);

        if (sessionJustStarted) {

            collaborationRoomService.activateRoom(
                    savedSession.getId());

            sessionRealtimeService.broadcastSessionStarted(
                    savedSession);
        }

        return mapToResponse(savedSession);
    }

    @Override
    @Transactional
    public SessionResponseDto scheduleSession(
            UUID sessionId,
            UUID userId,
            ScheduleSessionRequestDto request) {

        ServiceSession session =
                getSessionEntity(sessionId);

        if (session.getStatus() != SessionStatus.CREATED) {

            throw new BusinessException(
                    "Session cannot be scheduled");
        }

        UUID requesterId =
                session.getAgreement()
                        .getRequester()
                        .getId();

        UUID providerId =
                session.getAgreement()
                        .getProvider()
                        .getId();

        if (!requesterId.equals(userId)
                && !providerId.equals(userId)) {

            throw new BusinessException(
                    "You are not part of this session");
        }

        applySchedule(
                session,
                userId,
                request.getScheduledAt());

        ServiceSession savedSession =
                sessionRepository.save(session);

        userActivityService.recordQualifyingActivity(
                userId,
                RewardQualifyingActivityType.SESSION_SCHEDULED,
                sessionId.toString()
        );

        negotiationTimelineService.createSystemMessage(
                savedSession.getAgreement().getNegotiation(),
                "Session scheduled for "
                        + request.getScheduledAt());

        UUID recipientId =
                savedSession.getAgreement()
                        .getRequester()
                        .getId()
                        .equals(userId)
                        ? savedSession.getAgreement()
                        .getProvider()
                        .getId()
                        : savedSession.getAgreement()
                        .getRequester()
                        .getId();

        notificationService.createNotification(
                recipientId,
                NotificationType.SESSION,
                "Session Scheduled",
                "A session has been scheduled for "
                        + request.getScheduledAt());

        sessionRealtimeService.broadcastSessionScheduled(
                savedSession);

        return mapToResponse(savedSession);
    }

    @Override
    @Transactional
    public SessionResponseDto rescheduleSession(
            UUID sessionId,
            UUID userId,
            RescheduleSessionRequestDto request) {

        ServiceSession session =
                getSessionEntity(sessionId);

        if (session.getStatus() != SessionStatus.SCHEDULED) {

            throw new BusinessException(
                    "Only scheduled sessions can be rescheduled");
        }

        UUID requesterId =
                session.getAgreement()
                        .getRequester()
                        .getId();

        UUID providerId =
                session.getAgreement()
                        .getProvider()
                        .getId();

        if (!requesterId.equals(userId)
                && !providerId.equals(userId)) {

            throw new BusinessException(
                    "You are not part of this session");
        }
        applySchedule(
                session,
                userId,
                request.getScheduledAt());

        ServiceSession savedSession =
                sessionRepository.save(session);

        negotiationTimelineService.createSystemMessage(
                savedSession.getAgreement().getNegotiation(),
                "Session rescheduled.");

        UUID recipientId =
                savedSession.getAgreement()
                        .getRequester()
                        .getId()
                        .equals(userId)
                        ? savedSession.getAgreement()
                        .getProvider()
                        .getId()
                        : savedSession.getAgreement()
                        .getRequester()
                        .getId();

        notificationService.createNotification(
                recipientId,
                NotificationType.SESSION,
                "Session Rescheduled",
                "The scheduled session has been updated.");

        sessionRealtimeService.broadcastSessionRescheduled(
                savedSession);

        return mapToResponse(savedSession);
    }

    @Override
    @Transactional
    public SessionResponseDto markWorkCompleted(
            UUID sessionId,
            UUID userId) {

        try {

        ServiceSession session =
                getSessionEntity(sessionId);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {

            throw new BusinessException(
                    "Session is not in progress");
        }

            ExchangeMode exchangeMode =
                    getExchangeMode(session);

            if (session.getAgreement().getReleaseMode()
                    == AgreementReleaseMode.MILESTONE_RELEASE) {
                throw new BusinessException(
                        "Milestone Release must use the milestone-specific workflow");
            }

            UUID requesterId =
                    session.getAgreement()
                            .getRequester()
                            .getId();

        UUID providerId =
                session.getAgreement()
                        .getProvider()
                        .getId();

        if (!requesterId.equals(userId)
                && !providerId.equals(userId)) {

            throw new BusinessException(
                    "You are not part of this session");
        }

        /*
         * TOKEN MODE
         */
        if (exchangeMode == ExchangeMode.TOKEN_BASED) {

            if (!providerId.equals(userId)) {

                throw new BusinessException(
                        "Only provider can mark work completed.");
            }

            if (session.getProviderCompletedAt() != null) {

                throw new BusinessException(
                        "Work already marked completed.");
            }

            session.setProviderCompletedAt(
                    Instant.now());
        }

        /*
         * HYBRID & DIRECT BARTER
         */
        else {

            if (providerId.equals(userId)) {

                if (session.getProviderCompletedAt() != null) {

                    throw new BusinessException(
                            "Work already marked completed.");
                }

                session.setProviderCompletedAt(
                        Instant.now());

            } else {

                if (session.getRequesterCompletedAt() != null) {

                    throw new BusinessException(
                            "Work already marked completed.");
                }

                session.setRequesterCompletedAt(
                        Instant.now());
            }
        }

        ServiceSession savedSession =
                sessionRepository.save(session);

            userActivityService.recordQualifyingActivity(
                    userId,
                    RewardQualifyingActivityType.SESSION_WORK_MARKED_COMPLETE,
                    sessionId.toString()
            );

        negotiationTimelineService.createSystemMessage(
                savedSession.getAgreement().getNegotiation(),
                "Work marked as completed.");



        UUID recipientId =
                savedSession.getAgreement()
                        .getRequester()
                        .getId()
                        .equals(userId)
                        ? savedSession.getAgreement()
                        .getProvider()
                        .getId()
                        : savedSession.getAgreement()
                        .getRequester()
                        .getId();

        notificationService.createNotification(
                recipientId,
                NotificationType.SESSION,
                "Work Completed",
                "The other participant marked the work as completed.");



        sessionRealtimeService.broadcastWorkCompleted(
                savedSession);



        return mapToResponse(savedSession);

    } catch (Exception e) {

        System.out.println("========== EXCEPTION ==========");
        e.printStackTrace();

        throw e;
    }
    }

    @Override
    @Transactional
    public SessionResponseDto confirmCompletion(
            UUID sessionId,
            UUID userId) {

        ServiceSession session =
                getSessionEntity(sessionId);

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {

            throw new BusinessException(
                    "Session is not in progress");
        }

        ExchangeMode exchangeMode =
                getExchangeMode(session);

        if (session.getAgreement().getReleaseMode()
                == AgreementReleaseMode.MILESTONE_RELEASE) {
            throw new BusinessException(
                    "Milestone Release must use the milestone-specific workflow");
        }

        UUID requesterId =
                session.getAgreement()
                        .getRequester()
                        .getId();

        UUID providerId =
                session.getAgreement()
                        .getProvider()
                        .getId();

        if (!requesterId.equals(userId)
                && !providerId.equals(userId)) {

            throw new BusinessException(
                    "You are not part of this session");
        }

        /*
         * TOKEN MODE
         */
        if (exchangeMode == ExchangeMode.TOKEN_BASED) {

            if (!requesterId.equals(userId)) {

                throw new BusinessException(
                        "Only requester can confirm completion.");
            }

            if (session.getProviderCompletedAt() == null) {

                throw new BusinessException(
                        "Provider has not completed the work.");
            }

            if (Boolean.TRUE.equals(session.getRequesterConfirmed())) {

                throw new BusinessException(
                        "Already confirmed.");
            }

            session.setRequesterConfirmed(true);
            session.setRequesterConfirmedAt(
                    Instant.now());

            completeSession(session);
        }

        /*
         * HYBRID & DIRECT
         */
        else {

            if (providerId.equals(userId)) {

                if (session.getRequesterCompletedAt() == null) {

                    throw new BusinessException(
                            "Other participant has not completed work.");
                }

                session.setProviderConfirmed(true);
                session.setProviderConfirmedAt(
                        Instant.now());

            } else {

                if (session.getProviderCompletedAt() == null) {

                    throw new BusinessException(
                            "Other participant has not completed work.");
                }

                session.setRequesterConfirmed(true);
                session.setRequesterConfirmedAt(
                        Instant.now());
            }

            if (Boolean.TRUE.equals(session.getProviderConfirmed())
                    && Boolean.TRUE.equals(session.getRequesterConfirmed())) {

                completeSession(session);
            }
        }

        ServiceSession savedSession =
                sessionRepository.save(session);

        userActivityService.recordQualifyingActivity(
                userId,
                RewardQualifyingActivityType.SESSION_COMPLETION_CONFIRMED,
                sessionId.toString()
        );

        if (savedSession.getStatus()
                == SessionStatus.COMPLETED) {

            trustAssessmentService.refreshAssessment(requesterId);

            trustAssessmentService.refreshAssessment(providerId);

            sessionRealtimeService.broadcastSessionCompleted(
                    savedSession);
        }

        // Process referral eligibility only after completion is saved.
        if (savedSession.getAgreement().getExchangeMode()
                == ExchangeMode.DIRECT_BARTER
                && savedSession.getAgreement().getNegotiation()
                .getProposedExchangeMode()
                == ExchangeMode.DIRECT_BARTER) {

            referralAttributionService.processCompletedDirectBarterSession(
                    savedSession
            );
        }

        return mapToResponse(savedSession);
    }

    @Override
    @Transactional
    public SessionResponseDto disputeSession(
            UUID sessionId,
            UUID userId,
            OpenDisputeRequestDto request) {

        if (request == null
                || request.getReason() == null
                || request.getReason().isBlank()) {

            throw new BusinessException(
                    "Dispute reason is required");
        }

        ServiceSession session = getSessionEntity(sessionId);

        validateParticipant(session);

        if (session.getStatus() == SessionStatus.DISPUTED) {
            throw new BusinessException(
                    "A dispute has already been opened for this session");
        }

        if (session.getAgreement().getReleaseMode()
                == AgreementReleaseMode.MILESTONE_RELEASE
                && session.getAgreement().getExchangeMode()
                != ExchangeMode.DIRECT_BARTER) {

            throw new BusinessException(
                    "General session disputes are temporarily disabled "
                            + "for Milestone Release. "
                            + "A milestone-specific dispute process is required.");
        }

        if (session.getStatus() == SessionStatus.COMPLETED
                || session.getStatus() == SessionStatus.CANCELLED) {

            throw new BusinessException(
                    "Session cannot be disputed");
        }

        ServiceAgreement agreement = session.getAgreement();

        UUID requesterId = agreement.getRequester().getId();
        UUID providerId = agreement.getProvider().getId();

        User claimant;
        User respondent;

        if (requesterId.equals(userId)) {
            claimant = agreement.getRequester();
            respondent = agreement.getProvider();
        } else if (providerId.equals(userId)) {
            claimant = agreement.getProvider();
            respondent = agreement.getRequester();
        } else {
            throw new BusinessException(
                    "You are not part of this session");
        }

        // Create the persistent dispute case.
        DisputeCase disputeCase = DisputeCase.builder()
                .session(session)
                .agreementVersionAtOpening(
                        agreement.getActiveVersion())
                .claimant(claimant)
                .respondent(respondent)
                .reason(request.getReason().trim())
                .status(DisputeCaseStatus.PANEL_ASSIGNMENT_PENDING)
                .build();

        DisputeCase savedDisputeCase =
                disputeCaseRepository.save(disputeCase);

        // Automatically select and persist five random panelists.
        // If assignment fails, the transaction should roll back.
        List<DisputePanelAssignment> panelAssignments =
                disputePanelAssignmentService.assignRandomPanel(
                        savedDisputeCase);

        for (DisputePanelAssignment assignment : panelAssignments) {
            notificationService.createNotification(
                    assignment.getPanelist().getId(),
                    NotificationType.SESSION,
                    "Dispute Panel Assignment",
                    "You have been randomly selected to review a dispute. "
                            + "The case will be available in the dispute panel."
            );
        }

        // Update the existing session and agreement states.
        session.setStatus(SessionStatus.DISPUTED);
        agreement.setStatus(AgreementStatus.DISPUTED);

        agreementRepository.save(agreement);

        ServiceSession savedSession =
                sessionRepository.save(session);

        negotiationTimelineService.createSystemMessage(
                savedSession.getAgreement().getNegotiation(),
                "Session disputed.");

        disputeEvidenceService.attachExistingEvidence(
                savedDisputeCase);

        UUID recipientId = requesterId.equals(userId)
                ? providerId
                : requesterId;

        notificationService.createNotification(
                recipientId,
                NotificationType.SESSION,
                "Session Disputed",
                "The session has been marked as disputed.");

        sessionRealtimeService.broadcastSessionDisputed(
                savedSession);

        SessionResponseDto response = mapToResponse(savedSession);
        response.setDisputeId(savedDisputeCase.getId());
        return response;
    }

    @Override
    @Transactional
    public SessionResponseDto cancelSession(
            UUID sessionId,
            UUID userId) {

        ServiceSession session =
                getSessionEntity(sessionId);

        validateParticipant(session);

        if (session.getStatus() == SessionStatus.COMPLETED
                || session.getStatus() == SessionStatus.CANCELLED) {

            throw new BusinessException(
                    "Session cannot be cancelled");
        }

        if (session.getStatus() == SessionStatus.DISPUTED) {
            throw new BusinessException(
                    "A disputed session cannot be cancelled or refunded "
                            + "through the cancellation workflow. "
                            + "The dispute must be resolved first.");
        }

        ServiceAgreement agreement =
                session.getAgreement();

        if (agreement.getReleaseMode()
                == AgreementReleaseMode.MILESTONE_RELEASE) {
            throw new BusinessException(
                    "Milestone Release cancellation requires milestone-specific refund resolution");
        }

        ExchangeMode exchangeMode =
                getExchangeMode(session);

        /*
         * TOKEN_BASED and HYBRID sessions use
         * blockchain escrow settlement.
         *
         * DIRECT_BARTER has no token escrow.
         */
        if (exchangeMode != ExchangeMode.DIRECT_BARTER) {

            String escrowId =
                    agreement.getBlockchainEscrowId();

            if (escrowId == null || escrowId.isBlank()) {

                throw new BusinessException(
                        "Blockchain escrow ID is missing for token refund");
            }

            Integer escrowAmount =
                    agreement.getEscrowLockedTokens();

            if (escrowAmount == null || escrowAmount <= 0) {

                throw new BusinessException(
                        "Escrow token amount must be greater than zero");
            }

            /*
             * Verify the application-side locked balance
             * before performing the refund.
             */
            WalletSummaryDto walletSummary =
                    walletService.getWalletSummary(
                            agreement.getRequester().getId());

            if (walletSummary.getLockedTokens() < escrowAmount) {

                throw new BusinessException(
                        "Application locked wallet balance is insufficient");
            }

            BlockchainEscrowService.EscrowData escrow =
                    blockchainEscrowService.getEscrow(escrowId);

            if (escrow.status() != 1) {
                throw new BusinessException(
                        "Blockchain escrow is not in LOCKED state");
            }

            /*
             * REAL blockchain REFUND.
             *
             * The blockchain transaction is performed first.
             * If it fails, the SQL wallet mirror is not changed.
             */
            blockchainEscrowTransactionService.sendRefund(
                    escrowId);



            /*
             * Blockchain REFUND succeeded.
             *
             * Synchronize the application-side wallet mirror.
             */
            walletService.refundTokens(
                    agreement.getRequester().getId(),
                    escrowAmount,
                    "Blockchain escrow refunded: " + escrowId);

            /*
             * The agreement no longer has tokens locked.
             */
            agreement.setEscrowLockedTokens(0);
        }

        /*
         * Only update the business state after
         * the blockchain settlement succeeds.
         */
        session.setStatus(
                SessionStatus.CANCELLED);

        session.setCancelledAt(
                Instant.now());

        agreement.setStatus(
                AgreementStatus.CANCELLED);

        agreementRepository.save(agreement);

        ServiceSession savedSession =
                sessionRepository.save(session);

        collaborationRoomService.closeRoom(
                savedSession.getId());

        negotiationTimelineService.createSystemMessage(
                savedSession.getAgreement().getNegotiation(),
                "Session cancelled.");
        UUID recipientId =
                savedSession.getAgreement()
                        .getRequester()
                        .getId()
                        .equals(userId)
                        ? savedSession.getAgreement()
                        .getProvider()
                        .getId()
                        : savedSession.getAgreement()
                        .getRequester()
                        .getId();

        notificationService.createNotification(
                recipientId,
                NotificationType.SESSION,
                "Session Cancelled",
                "The session has been cancelled.");

        sessionRealtimeService.broadcastSessionCancelled(
                savedSession);

        return mapToResponse(savedSession);
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

        dto.setSessionId(
                session.getId());

        dto.setAgreementId(
                session.getAgreement().getId());

        dto.setNegotiationId(
                session.getAgreement()
                        .getNegotiation()
                        .getId());

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

        dto.setStatus(
                session.getStatus());

        dto.setExchangeMode(
                session.getAgreement()
                        .getExchangeMode());


        // Session mode selection and mutual agreement
        dto.setRequesterSelectedMode(
                session.getRequesterSelectedMode());

        dto.setProviderSelectedMode(
                session.getProviderSelectedMode());

        dto.setAgreedSessionMode(
                session.getAgreedSessionMode());

        dto.setRequesterModeSelectedAt(
                session.getRequesterModeSelectedAt());

        dto.setProviderModeSelectedAt(
                session.getProviderModeSelectedAt());

        dto.setModeAgreedAt(
                session.getModeAgreedAt());

        dto.setProviderStarted(
                session.getProviderStarted());

        dto.setRequesterStarted(
                session.getRequesterStarted());

        dto.setProviderConfirmed(
                session.getProviderConfirmed());

        dto.setRequesterConfirmed(
                session.getRequesterConfirmed());

        dto.setScheduledAt(
                session.getScheduledAt());

        dto.setScheduleAcceptedAt(
                session.getScheduleAcceptedAt());

        dto.setStartedAt(
                session.getStartedAt());

        dto.setProviderCompletedAt(
                session.getProviderCompletedAt());

        dto.setRequesterCompletedAt(
                session.getRequesterCompletedAt());

        dto.setProviderConfirmedAt(
                session.getProviderConfirmedAt());

        dto.setRequesterConfirmedAt(
                session.getRequesterConfirmedAt());

        dto.setCompletedAt(
                session.getCompletedAt());

        dto.setCancelledAt(
                session.getCancelledAt());

        dto.setCreatedAt(
                session.getCreatedAt());

        return dto;
    }


    @Override
    @Transactional
    public SessionResponseDto acceptSchedule(
            UUID sessionId,
            UUID userId) {

        ServiceSession session =
                getSessionEntity(sessionId);

        if (session.getStatus() != SessionStatus.SCHEDULED) {

            throw new BusinessException(
                    "Session is not scheduled");
        }

        UUID requesterId =
                session.getAgreement()
                        .getRequester()
                        .getId();

        UUID providerId =
                session.getAgreement()
                        .getProvider()
                        .getId();

        if (!requesterId.equals(userId)
                && !providerId.equals(userId)) {

            throw new BusinessException(
                    "You are not part of this session");
        }

        if (session.getScheduledBy() != null
                && session.getScheduledBy()
                .getId()
                .equals(userId)) {

            throw new BusinessException(
                    "You cannot accept your own schedule");
        }

        if (session.getScheduleAcceptedAt() != null) {

            throw new BusinessException(
                    "Schedule has already been accepted");
        }

        session.setScheduleAcceptedAt(
                Instant.now());

        ServiceSession savedSession =
                sessionRepository.save(session);

        userActivityService.recordQualifyingActivity(
                userId,
                RewardQualifyingActivityType.SESSION_SCHEDULE_ACCEPTED,
                sessionId.toString()
        );

        negotiationTimelineService.createSystemMessage(
                savedSession.getAgreement().getNegotiation(),
                "Schedule accepted.");

        UUID recipientId =
                savedSession.getAgreement()
                        .getRequester()
                        .getId()
                        .equals(userId)
                        ? savedSession.getAgreement()
                        .getProvider()
                        .getId()
                        : savedSession.getAgreement()
                        .getRequester()
                        .getId();

        notificationService.createNotification(
                recipientId,
                NotificationType.SESSION,
                "Schedule Accepted",
                "The session schedule has been accepted.");

        sessionRealtimeService.broadcastScheduleAccepted(
                savedSession);

        return mapToResponse(savedSession);
    }


    private void applySchedule(
            ServiceSession session,
            UUID userId,
            Instant scheduledAt) {

        User scheduledBy =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        session.setScheduledAt(
                scheduledAt);

        session.setScheduledBy(
                scheduledBy);

        session.setScheduleAcceptedAt(
                null);

        session.setProviderStarted(
                false);

        session.setRequesterStarted(
                false);

        session.setStatus(
                SessionStatus.SCHEDULED);
    }

    private ExchangeMode getExchangeMode(
            ServiceSession session) {

        return session.getAgreement()
                .getNegotiation()
                .getProposedExchangeMode();
    }


    @Override
    @Transactional
    public boolean completeMilestoneSessionIfFinal(UUID agreementId) {

        if (agreementId == null) {
            throw new BusinessException("Agreement ID is required");
        }

        ServiceAgreement agreement =
                agreementRepository.findByIdForFinalizationUpdate(agreementId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agreement not found"));

        if (agreement.getReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE
                || agreement.getExchangeMode()
                == ExchangeMode.DIRECT_BARTER) {
            throw new BusinessException(
                    "Final milestone completion is only available for Milestone Release agreements");
        }

        ServiceSession session =
                sessionRepository.findByAgreement(agreement)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Session not found for agreement"));

        /*
         * Idempotent completion: both records must already be complete.
         */
        if (agreement.getStatus() == AgreementStatus.COMPLETED
                && session.getStatus() == SessionStatus.COMPLETED) {
            return true;
        }

        if (agreement.getStatus() == AgreementStatus.DISPUTED) {
            throw new BusinessException(
                    "A disputed agreement cannot be finalized; reconciliation is required");
        }

        if (agreement.getStatus() == AgreementStatus.COMPLETED
                || session.getStatus() == SessionStatus.COMPLETED) {
            throw new BusinessException(
                    "Agreement and session completion states do not match; reconciliation is required");
        }

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException(
                    "The session must be in progress to finalize the last milestone");
        }

        if (agreement.getActiveVersion() == null
                || agreement.getActiveVersion().getStatus()
                != AgreementVersionStatus.ACTIVE
                || agreement.getActiveVersion().getReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE) {
            throw new BusinessException(
                    "An active Milestone Release version is required");
        }

        List<AgreementVersionMilestone> plan =
                agreementVersionMilestoneRepository
                        .findByVersion_IdOrderByMilestoneNumberAsc(
                                agreement.getActiveVersion().getId());

        if (plan.size() < 2 || plan.size() > 5) {
            throw new BusinessException(
                    "The active version must contain between 2 and 5 milestones");
        }

        /*
         * Do not complete the session if any milestone is unreleased,
         * has an invalid position, or still carries locked BLT.
         */
        for (int i = 0; i < plan.size(); i++) {

            AgreementVersionMilestone item = plan.get(i);

            if (item.getMilestone() == null
                    || !Integer.valueOf(i + 1).equals(
                    item.getMilestoneNumber())) {
                throw new BusinessException(
                        "The active milestone plan is invalid");
            }

            var milestone = item.getMilestone();

            if (milestone.getStatus()
                    != com.barterlink.agreements.enums.AgreementMilestoneStatus.RELEASED
                    || milestone.getReleasedAt() == null
                    || milestone.getEscrowLockedTokens() == null
                    || milestone.getEscrowLockedTokens() != 0) {
                return false;
            }
        }

        if (agreement.getEscrowLockedTokens() == null
                || agreement.getEscrowLockedTokens() != 0) {
            throw new BusinessException(
                    "The agreement still records locked tokens; reconciliation is required");
        }

        Instant now = Instant.now();

        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(now);

        agreement.setStatus(AgreementStatus.COMPLETED);
        agreement.setCompletedAt(now);

        ServiceSession savedSession =
                sessionRepository.save(session);

        agreementRepository.save(agreement);

        negotiationTimelineService.createSystemMessage(
                agreement.getNegotiation(),
                "All milestones have been released. "
                        + "The session is completed and the conversation is now read-only.");

        notificationService.createNotification(
                agreement.getRequester().getId(),
                NotificationType.SESSION,
                "Session Completed",
                "All milestones were completed and released successfully.");

        notificationService.createNotification(
                agreement.getProvider().getId(),
                NotificationType.SESSION,
                "Session Completed",
                "All milestones were completed and released successfully.");

        trustAssessmentService.refreshAssessment(
                agreement.getRequester().getId());

        trustAssessmentService.refreshAssessment(
                agreement.getProvider().getId());

        sessionRealtimeService.broadcastSessionCompleted(savedSession);

        return true;
    }


    private void completeSession(
            ServiceSession session) {

        ServiceAgreement agreement =
                session.getAgreement();

        if (agreement.getReleaseMode()
                == AgreementReleaseMode.MILESTONE_RELEASE) {
            throw new BusinessException(
                    "Milestone Release must use the milestone-specific workflow");
        }

        ExchangeMode exchangeMode =
                getExchangeMode(session);

        /*
         * TOKEN_BASED and HYBRID exchanges use
         * blockchain escrow settlement.
         *
         * DIRECT_BARTER has no token escrow.
         */
        if (exchangeMode != ExchangeMode.DIRECT_BARTER) {

            String escrowId =
                    agreement.getBlockchainEscrowId();

            if (escrowId == null || escrowId.isBlank()) {

                throw new BusinessException(
                        "Blockchain escrow ID is missing for token settlement");
            }

            Integer escrowAmount =
                    agreement.getEscrowLockedTokens();

            if (escrowAmount == null || escrowAmount <= 0) {

                throw new BusinessException(
                        "Escrow token amount must be greater than zero");
            }

            /*
             * Verify that the application-side locked balance
             * is available before touching the blockchain.
             *
             * This does not perform any blockchain transaction.
             */
            WalletSummaryDto walletSummary =
                    walletService.getWalletSummary(
                            agreement.getRequester().getId());

            if (walletSummary.getLockedTokens() < escrowAmount) {

                throw new BusinessException(
                        "Application locked wallet balance is insufficient");
            }

            /*
             * REAL blockchain RELEASE.
             *
             * sendRelease() already handles:
             * - blockchain transaction creation
             * - transaction tracking
             * - submission
             * - confirmation
             * - failure handling
             */
            blockchainEscrowTransactionService.sendRelease(
                    escrowId);

            /*
             * Blockchain RELEASE succeeded.
             *
             * Synchronize the application-side wallet mirror.
             */
            walletService.transferLockedTokens(
                    agreement.getRequester().getId(),
                    agreement.getProvider().getId(),
                    escrowAmount,
                    "Blockchain escrow released: " + escrowId);

            /*
             * The agreement no longer has tokens locked in escrow.
             */
            agreement.setEscrowLockedTokens(0);
        }

        /*
         * Only mark the business objects completed
         * after the token settlement succeeds.
         */
        session.setStatus(
                SessionStatus.COMPLETED);

        session.setCompletedAt(
                Instant.now());

        agreement.setStatus(
                AgreementStatus.COMPLETED);

        agreement.setCompletedAt(
                Instant.now());

        /*
         * Queue Direct Barter rewards only after successful server-side
         * session completion. Both the agreed agreement mode and the
         * mode used by the existing session workflow must indicate
         * Direct Barter.
         *
         * Reward creation is an SQL operation here. Blockchain submission
         * happens later in the reward scheduler.
         */
        if (exchangeMode == ExchangeMode.DIRECT_BARTER
                && agreement.getExchangeMode() == ExchangeMode.DIRECT_BARTER) {

            rewardClaimQueueService.enqueueDirectBarterCompletion(
                    session.getId(),
                    agreement.getRequester(),
                    agreement.getProvider()
            );
        }

        negotiationTimelineService.createSystemMessage(
                agreement.getNegotiation(),
                "Session completed. Conversation is now read-only.");

        notificationService.createNotification(
                agreement.getRequester().getId(),
                NotificationType.SESSION,
                "Session Completed",
                "The session has been completed successfully.");

        notificationService.createNotification(
                agreement.getProvider().getId(),
                NotificationType.SESSION,
                "Session Completed",
                "The session has been completed successfully.");
    }
}
