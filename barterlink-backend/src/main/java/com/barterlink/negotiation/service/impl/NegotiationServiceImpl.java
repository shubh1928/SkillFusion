package com.barterlink.negotiation.service.impl;

import com.barterlink.agreements.service.AgreementService;
import com.barterlink.agreements.service.AgreementVersionService;
import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.interest.entity.RequestInterest;
import com.barterlink.interest.enums.InterestStatus;
import com.barterlink.interest.repository.RequestInterestRepository;
import com.barterlink.marketplace.entity.ServiceRequest;
import com.barterlink.marketplace.enums.ServiceRequestStatus;
import com.barterlink.marketplace.repository.ServiceRequestRepository;
import com.barterlink.negotiation.dto.request.SendNegotiationMessageRequestDto;
import com.barterlink.negotiation.dto.request.UpdateNegotiationProposalRequestDto;
import com.barterlink.negotiation.dto.response.NegotiationMessageResponseDto;
import com.barterlink.negotiation.dto.response.NegotiationResponseDto;
import com.barterlink.negotiation.entity.Negotiation;
import com.barterlink.negotiation.entity.NegotiationMessage;
import com.barterlink.negotiation.enums.NegotiationStatus;
import com.barterlink.negotiation.repository.NegotiationMessageRepository;
import com.barterlink.negotiation.repository.NegotiationRepository;
import com.barterlink.negotiation.service.NegotiationService;
import com.barterlink.negotiation.service.NegotiationTimelineService;
import com.barterlink.notifications.service.NotificationService;
import com.barterlink.notifications.enums.NotificationType;
import com.barterlink.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.barterlink.websocket.dto.ChatMessageDto;
import com.barterlink.websocket.service.WebSocketService;
import com.barterlink.websocket.constants.WebSocketDestinationConstants;
import com.barterlink.agreements.enums.AgreementReleaseMode;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.negotiation.dto.request.SelectAgreementReleaseModeRequestDto;
import com.barterlink.agreements.entity.AgreementVersion;
import com.barterlink.agreements.service.impl.MilestoneFundingCoordinator;
import com.barterlink.agreements.service.impl.MilestoneFundingPreparationService;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.barterlink.auth.services.UserActivityService;
import com.barterlink.rewards.enums.RewardQualifyingActivityType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class NegotiationServiceImpl implements NegotiationService {

    private final NegotiationRepository negotiationRepository;

    private final NegotiationMessageRepository negotiationMessageRepository;

    private final RequestInterestRepository requestInterestRepository;

    private final ServiceRequestRepository serviceRequestRepository;

    private final UserRepository userRepository;

    private final UserActivityService userActivityService;

    private final AgreementService agreementService;

    private final WalletService walletService;

    private final NegotiationTimelineService negotiationTimelineService;

    private final NotificationService notificationService;

    private final WebSocketService webSocketService;

    private final AgreementVersionService agreementVersionService;

    private final MilestoneFundingPreparationService milestoneFundingPreparationService;

    private final MilestoneFundingCoordinator milestoneFundingCoordinator;

    @Override
    public NegotiationResponseDto openNegotiation(
            UUID interestId) {

        RequestInterest interest =
                requestInterestRepository.findById(interestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Interest not found"));

        return negotiationRepository
                .findByServiceRequestAndProvider(
                        interest.getServiceRequest(),
                        interest.getProvider())
                .map(this::mapToResponse)
                .orElseGet(() -> {

                    Negotiation negotiation =
                            buildNegotiation(
                                    interest.getServiceRequest(),
                                    interest.getProvider());

                    Negotiation savedNegotiation =
                            negotiationRepository.save(negotiation);

                    /*
                     * Count only a newly persisted negotiation.
                     * The qualifying activity joins the existing service
                     * transaction, so it rolls back if creation fails.
                     */
                    userActivityService.recordCurrentUserQualifyingActivity(
                            RewardQualifyingActivityType.NEGOTIATION_CREATED,
                            savedNegotiation.getId().toString()
                    );

                    return mapToResponse(savedNegotiation);
                });
    }


    @Override
    public NegotiationResponseDto continueChat(
            UUID requestId,
            UUID providerId) {

        ServiceRequest serviceRequest =
                serviceRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Request not found"));

        User provider =
                userRepository.findById(providerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Provider not found"));

        Negotiation negotiation =
                negotiationRepository
                        .findByServiceRequestAndProvider(
                                serviceRequest,
                                provider)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "No negotiation exists yet. Please express interest first."));

        return mapToResponse(negotiation);
    }

    @Override
    public NegotiationResponseDto getNegotiation(
            UUID negotiationId) {

        Negotiation negotiation =
                negotiationRepository.findById(negotiationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Negotiation not found"));

        return mapToResponse(negotiation);
    }

    @Override
    public List<NegotiationResponseDto> getRequestNegotiations(
            UUID requestId) {

        ServiceRequest serviceRequest =
                serviceRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Request not found"));

        return negotiationRepository
                .findByServiceRequest(serviceRequest)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<NegotiationResponseDto> getProviderNegotiations(
            UUID providerId) {

        User provider =
                userRepository.findById(providerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Provider not found"));

        return negotiationRepository
                .findByProvider(provider)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<NegotiationMessageResponseDto> getMessages(
            UUID negotiationId) {

        Negotiation negotiation =
                negotiationRepository.findById(negotiationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Negotiation not found"));

        return negotiationMessageRepository
                .findByNegotiationOrderByCreatedAtAsc(
                        negotiation)
                .stream()
                .map(this::mapMessageToResponse)
                .toList();
    }

    @Override
    public NegotiationMessageResponseDto sendMessage(
            UUID negotiationId,
            UUID senderId,
            SendNegotiationMessageRequestDto request) {

        Negotiation negotiation =
                negotiationRepository.findById(negotiationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Negotiation not found"));

        User sender =
                userRepository.findById(senderId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        if (!sender.getId().equals(negotiation.getRequester().getId())
                && !sender.getId().equals(negotiation.getProvider().getId())) {

            throw new BusinessException(
                    "You are not part of this negotiation");
        }

        NegotiationMessage message =
                NegotiationMessage.builder()
                        .negotiation(negotiation)
                        .sender(sender)
                        .message(request.getMessage())
                        .build();

        NegotiationMessage savedMessage =
                negotiationMessageRepository.save(message);

        userActivityService.recordQualifyingActivity(
                senderId,
                RewardQualifyingActivityType.NEGOTIATION_MESSAGE_SENT,
                savedMessage.getId().toString()
        );

        ChatMessageDto chatMessage =
                mapToChatDto(savedMessage);

        webSocketService.sendNegotiationMessage(
                WebSocketDestinationConstants.NEGOTIATION_TOPIC
                        + negotiation.getId(),
                chatMessage);

        return mapMessageToResponse(savedMessage);
    }

    @Override
    public void addSystemMessage(
            UUID negotiationId,
            String message) {

        negotiationTimelineService.createSystemMessage(
                negotiationId,
                message);
    }

    @Override
    public NegotiationResponseDto updateProposal(
            UUID negotiationId,
            UUID userId,
            UpdateNegotiationProposalRequestDto request) {

        Negotiation negotiation =
                negotiationRepository.findById(negotiationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Negotiation not found"));

        if (negotiation.getStatus() != NegotiationStatus.ACTIVE) {

            throw new BusinessException(
                    "Proposal cannot be updated in the current negotiation state");
        }

        if (!negotiation.getRequester().getId().equals(userId)
                && !negotiation.getProvider().getId().equals(userId)) {

            throw new BusinessException(
                    "You are not part of this negotiation");
        }

        boolean proposalChanged =
                !Objects.equals(
                        negotiation.getProposedOfferTitle(),
                        request.getOfferTitle()
                )
                        || !Objects.equals(
                        negotiation.getProposedOfferDescription(),
                        request.getOfferDescription()
                )
                        || !Objects.equals(
                        negotiation.getProposedRequestTitle(),
                        request.getRequestTitle()
                )
                        || !Objects.equals(
                        negotiation.getProposedRequestDescription(),
                        request.getRequestDescription()
                )
                        || !Objects.equals(
                        negotiation.getProposedExchangeMode(),
                        request.getExchangeMode()
                )
                        || !Objects.equals(
                        negotiation.getProposedEstimatedHours(),
                        request.getEstimatedHours()
                )
                        || !Objects.equals(
                        negotiation.getProposedTokenAmount(),
                        request.getTokenAmount()
                );

        negotiation.setProposedOfferTitle(
                request.getOfferTitle());

        negotiation.setProposedOfferDescription(
                request.getOfferDescription());

        negotiation.setProposedRequestTitle(
                request.getRequestTitle());

        negotiation.setProposedRequestDescription(
                request.getRequestDescription());

        negotiation.setProposedExchangeMode(
                request.getExchangeMode());

        /*
         * Direct Barter does not use token-release modes.
         * Clear stale choices if the exchange mode changes to Direct Barter.
         */
        if (request.getExchangeMode() == ExchangeMode.DIRECT_BARTER) {

            negotiation.setRequesterReleaseMode(null);
            negotiation.setProviderReleaseMode(null);

            negotiation.setRequesterMilestoneCount(null);
            negotiation.setProviderMilestoneCount(null);
        }

        negotiation.setProposedEstimatedHours(
                request.getEstimatedHours());

        negotiation.setProposedTokenAmount(
                request.getTokenAmount());

        Negotiation savedNegotiation =
                negotiationRepository.save(negotiation);

        if (proposalChanged) {
            userActivityService.recordQualifyingActivity(
                    userId,
                    RewardQualifyingActivityType.NEGOTIATION_PROPOSAL_UPDATED,
                    savedNegotiation.getId().toString()
            );
        }

        negotiationTimelineService.createSystemMessage(
                savedNegotiation,
                "Current deal updated.");

        UUID recipientId =
                savedNegotiation.getRequester()
                        .getId()
                        .equals(userId)
                        ? savedNegotiation.getProvider().getId()
                        : savedNegotiation.getRequester().getId();

        notificationService.createNotification(
                recipientId,
                NotificationType.NEGOTIATION,
                "Proposal Updated",
                "The current deal proposal has been updated.");

        return mapToResponse(savedNegotiation);
    }

    @Override
    public NegotiationResponseDto selectAgreementReleaseMode(
            UUID negotiationId,
            UUID userId,
            SelectAgreementReleaseModeRequestDto request) {

        Negotiation negotiation =
                negotiationRepository.findById(negotiationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Negotiation not found"));



        if (negotiation.getStatus() != NegotiationStatus.ACTIVE
                && negotiation.getStatus()
                != NegotiationStatus.SESSION_REQUESTED) {

            throw new BusinessException(
                    "Release mode can no longer be changed in the current negotiation state");
        }

        boolean isRequester =
                negotiation.getRequester().getId().equals(userId);

        boolean isProvider =
                negotiation.getProvider().getId().equals(userId);

        if (!isRequester && !isProvider) {
            throw new BusinessException(
                    "You are not part of this negotiation");
        }

        if (request == null || request.getReleaseMode() == null) {
            throw new BusinessException(
                    "Release mode is required");
        }

        ExchangeMode exchangeMode =
                negotiation.getProposedExchangeMode();

        if (exchangeMode == null
                || exchangeMode == ExchangeMode.DIRECT_BARTER) {

            throw new BusinessException(
                    "Release-mode selection applies only to Token Based and Hybrid exchanges");
        }

        AgreementReleaseMode selectedMode =
                request.getReleaseMode();

        Integer selectedMilestoneCount =
                request.getMilestoneCount();

        AgreementReleaseMode previousSelectedMode =
                isRequester
                        ? negotiation.getRequesterReleaseMode()
                        : negotiation.getProviderReleaseMode();

        Integer previousSelectedMilestoneCount =
                isRequester
                        ? negotiation.getRequesterMilestoneCount()
                        : negotiation.getProviderMilestoneCount();

        boolean releaseSelectionChanged =
                !Objects.equals(
                        previousSelectedMode,
                        selectedMode
                )
                        || !Objects.equals(
                        previousSelectedMilestoneCount,
                        selectedMilestoneCount
                );

        if (selectedMode == AgreementReleaseMode.DIRECT_RELEASE) {

            if (selectedMilestoneCount != null) {
                throw new BusinessException(
                        "Milestone count must be omitted for Direct Release");
            }

        } else if (selectedMode == AgreementReleaseMode.MILESTONE_RELEASE) {

            if (selectedMilestoneCount == null
                    || selectedMilestoneCount < 2
                    || selectedMilestoneCount > 5) {

                throw new BusinessException(
                        "Milestone Release requires between 2 and 5 milestones");
            }

        } else {
            throw new BusinessException(
                    "Unsupported release mode");
        }

        /*
         * Do not allow release-mode changes after agreement
         * version history has started.
         */
        if (agreementVersionService.hasAnyVersion(negotiation.getId())) {
            throw new BusinessException(
                    "Release mode cannot be changed after an agreement version has been created");
        }

        /*
         * Update only the authenticated participant's selection.
         * Never overwrite the other participant's selection.
         */
        if (isRequester) {

            negotiation.setRequesterReleaseMode(selectedMode);

            negotiation.setRequesterMilestoneCount(
                    selectedMilestoneCount);

        } else {

            negotiation.setProviderReleaseMode(selectedMode);

            negotiation.setProviderMilestoneCount(
                    selectedMilestoneCount);
        }

        Negotiation savedNegotiation =
                negotiationRepository.save(negotiation);

        if (releaseSelectionChanged) {
            userActivityService.recordQualifyingActivity(
                    userId,
                    RewardQualifyingActivityType.NEGOTIATION_RELEASE_MODE_SELECTED,
                    savedNegotiation.getId().toString()
            );
        }

        return mapToResponse(savedNegotiation);
    }

    @Override
    public NegotiationResponseDto requestSession(
            UUID negotiationId,
            UUID providerId) {

        Negotiation negotiation =
                negotiationRepository.findById(negotiationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Negotiation not found"));

        if (!negotiation.getProvider().getId().equals(providerId)) {
            throw new BusinessException(
                    "Only the provider can request a session");
        }

        if (negotiation.getStatus() != NegotiationStatus.ACTIVE) {
            throw new BusinessException(
                    "Session request is not allowed in the current negotiation state");
        }

        negotiation.setStatus(
                NegotiationStatus.SESSION_REQUESTED);

        Negotiation savedNegotiation =
                negotiationRepository.save(negotiation);

        negotiationTimelineService.createSystemMessage(
                savedNegotiation,
                negotiation.getProvider().getName()
                        + " requested to start the session.");

        notificationService.createNotification(
                savedNegotiation.getRequester().getId(),
                NotificationType.SESSION,
                "Session Requested",
                savedNegotiation.getProvider().getName()
                        + " requested to start the session.");

        userActivityService.recordQualifyingActivity(
                providerId,
                RewardQualifyingActivityType.SESSION_REQUEST_SUBMITTED,
                savedNegotiation.getId().toString()
        );

        return mapToResponse(savedNegotiation);
    }

    @Override
    public NegotiationResponseDto acceptSessionRequest(
            UUID negotiationId,
            UUID requesterId) {

        Negotiation negotiation =
                negotiationRepository.findById(negotiationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Negotiation not found"));

        if (!negotiation.getRequester()
                .getId()
                .equals(requesterId)) {

            throw new BusinessException(
                    "Only the requester can accept the session request");
        }

        if (negotiation.getStatus()
                != NegotiationStatus.SESSION_REQUESTED) {

            throw new BusinessException(
                    "Negotiation is not waiting for session acceptance");
        }

        /*
         * New negotiations require compatible release-mode selections.
         * Legacy rows with a NULL flag retain their previous workflow.
         * Direct Barter does not use release-mode selection.
         */
        AgreementVersion initialVersion = null;

        if (Boolean.TRUE.equals(
                negotiation.getReleaseModeSelectionRequired())
                && negotiation.getProposedExchangeMode()
                != ExchangeMode.DIRECT_BARTER) {

            AgreementReleaseMode requesterMode =
                    negotiation.getRequesterReleaseMode();

            AgreementReleaseMode providerMode =
                    negotiation.getProviderReleaseMode();

            if (requesterMode == null || providerMode == null) {
                throw new BusinessException(
                        "Both participants must select a release mode before accepting the session");
            }

            if (requesterMode != providerMode) {
                throw new BusinessException(
                        "Release modes do not match. Both participants must select the same release mode");
            }

            if (requesterMode == AgreementReleaseMode.MILESTONE_RELEASE) {

                Integer requesterCount =
                        negotiation.getRequesterMilestoneCount();

                Integer providerCount =
                        negotiation.getProviderMilestoneCount();

                if (requesterCount == null
                        || providerCount == null
                        || !requesterCount.equals(providerCount)
                        || requesterCount < 2
                        || requesterCount > 5) {
                    throw new BusinessException(
                            "Both participants must select the same milestone count");
                }
            }

            initialVersion =
                    agreementVersionService.requireReadyInitialVersion(
                            negotiation.getId());

            if (initialVersion.getReleaseMode() != requesterMode) {
                throw new BusinessException(
                        "The initial agreement version must match both participants' selected release mode");
            }
        }

        /*
         * Fail closed if legacy/null selection metadata contains a stale
         * Milestone Release choice without a mutually accepted initial version.
         */
        if (initialVersion == null
                && (negotiation.getRequesterReleaseMode()
                == AgreementReleaseMode.MILESTONE_RELEASE
                || negotiation.getProviderReleaseMode()
                == AgreementReleaseMode.MILESTONE_RELEASE)) {

            throw new BusinessException(
                    "Milestone Release requires matching selections and a mutually accepted initial version");
        }

        negotiation.setStatus(
                NegotiationStatus.ACCEPTED);

        negotiation.setAcceptedAt(
                Instant.now());

        Negotiation savedNegotiation =
                negotiationRepository.save(
                        negotiation);

        ServiceRequest serviceRequest =
                savedNegotiation.getServiceRequest();


        if (initialVersion != null
                && initialVersion.getReleaseMode()
                == AgreementReleaseMode.MILESTONE_RELEASE) {

            /*
             * Prepare the agreement, session, milestone and wallet reservation
             * inside the current acceptance transaction. Do not submit the
             * blockchain transaction before the database transaction commits.
             */
            MilestoneFundingPreparationService.PreparedMilestoneFunding fundingPlan =
                    milestoneFundingPreparationService.prepareInitialFunding(
                            savedNegotiation.getId(),
                            initialVersion.getId());

            /*
             * Run blockchain funding only after acceptance, the agreement,
             * session, milestone and wallet reservation have committed.
             */
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {

                        @Override
                        public void afterCommit() {

                            milestoneFundingCoordinator.fundPreparedMilestone(
                                    fundingPlan);
                        }
                    });

        } else {

            /*
             * Keep Direct Release and the existing non-milestone workflow
             * on the original agreement creation path.
             */
            agreementService.createAgreement(
                    savedNegotiation);
        }

        serviceRequest.setStatus(
                ServiceRequestStatus.ACCEPTED);

        serviceRequestRepository.save(
                serviceRequest);

        closeOtherNegotiations(
                savedNegotiation);

        closeOtherInterests(
                savedNegotiation);

        negotiationTimelineService.createSystemMessage(
                savedNegotiation,
                "Session request accepted. Agreement created.");

        notificationService.createNotification(
                savedNegotiation.getProvider().getId(),
                NotificationType.AGREEMENT,
                "Session Accepted",
                "Your session request has been accepted. The agreement has been created.");


        userActivityService.recordQualifyingActivity(
                requesterId,
                RewardQualifyingActivityType.SESSION_REQUEST_ACCEPTED,
                savedNegotiation.getId().toString()
        );

        return mapToResponse(
                savedNegotiation);
    }

    @Override
    public NegotiationResponseDto rejectSessionRequest(
            UUID negotiationId,
            UUID requesterId) {

        throw new UnsupportedOperationException(
                "Not implemented yet");
    }

    @Override
    public NegotiationResponseDto cancelNegotiation(
            UUID negotiationId,
            UUID userId) {

        throw new UnsupportedOperationException(
                "Not implemented yet");
    }


    private NegotiationResponseDto mapToResponse(
            Negotiation negotiation) {

        NegotiationResponseDto dto =
                new NegotiationResponseDto();

        dto.setNegotiationId(
                negotiation.getId());

        dto.setRequestId(
                negotiation.getServiceRequest().getId());

        dto.setRequesterId(
                negotiation.getRequester().getId());

        dto.setProviderId(
                negotiation.getProvider().getId());

        dto.setStatus(
                negotiation.getStatus());

        dto.setOfferTitle(
                negotiation.getProposedOfferTitle());

        dto.setOfferDescription(
                negotiation.getProposedOfferDescription());

        dto.setRequestTitle(
                negotiation.getProposedRequestTitle());

        dto.setRequestDescription(
                negotiation.getProposedRequestDescription());

        dto.setExchangeMode(
                negotiation.getProposedExchangeMode());

        dto.setEstimatedHours(
                negotiation.getProposedEstimatedHours());

        dto.setTokenAmount(
                negotiation.getProposedTokenAmount());

        dto.setCreatedAt(
                negotiation.getCreatedAt());

        dto.setRequesterReleaseMode(
                negotiation.getRequesterReleaseMode());

        dto.setProviderReleaseMode(
                negotiation.getProviderReleaseMode());

        dto.setRequesterMilestoneCount(
                negotiation.getRequesterMilestoneCount());

        dto.setProviderMilestoneCount(
                negotiation.getProviderMilestoneCount());

        dto.setReleaseModeSelectionRequired(
                negotiation.getReleaseModeSelectionRequired());

        return dto;
    }



    //helper

    private NegotiationMessageResponseDto mapMessageToResponse(
            NegotiationMessage message) {

        NegotiationMessageResponseDto dto =
                new NegotiationMessageResponseDto();

        dto.setMessageId(
                message.getId());

        if (message.getSender() != null) {

            dto.setSenderId(
                    message.getSender().getId());

            dto.setSenderName(
                    message.getSender().getName());

            dto.setSenderImage(
                    message.getSender().getImage());
        }

        dto.setMessageType(
                message.getMessageType());

        dto.setMessage(
                message.getMessage());

        dto.setCreatedAt(
                message.getCreatedAt());

        return dto;
    }




    //helper method

    private Negotiation buildNegotiation(
            ServiceRequest serviceRequest,
            User provider) {

        return Negotiation.builder()
                .serviceRequest(serviceRequest)
                .requester(serviceRequest.getRequester())
                .provider(provider)

                .proposedOfferTitle(
                        serviceRequest.getOfferTitle())

                .proposedOfferDescription(
                        serviceRequest.getOfferDescription())

                .proposedRequestTitle(
                        serviceRequest.getRequestTitle())

                .proposedRequestDescription(
                        serviceRequest.getRequestDescription())

                .proposedExchangeMode(
                        serviceRequest.getExchangeMode())

                .proposedEstimatedHours(
                        serviceRequest.getEstimatedHours())

                .proposedTokenAmount(
                        serviceRequest.getTokenAmount())

                .build();
    }


    //helper
    private void closeOtherNegotiations(
            Negotiation acceptedNegotiation) {

        List<Negotiation> negotiations =
                negotiationRepository.findByServiceRequest(
                        acceptedNegotiation.getServiceRequest());

        for (Negotiation negotiation : negotiations) {

            if (negotiation.getId().equals(
                    acceptedNegotiation.getId())) {
                continue;
            }

            if (negotiation.getStatus() == NegotiationStatus.ACCEPTED
                    || negotiation.getStatus() == NegotiationStatus.CANCELLED
                    || negotiation.getStatus() == NegotiationStatus.CLOSED) {
                continue;
            }

            negotiation.setStatus(
                    NegotiationStatus.CANCELLED);

            negotiation.setClosedAt(
                    Instant.now());

            negotiationRepository.save(
                    negotiation);

            negotiationTimelineService.createSystemMessage(
                    negotiation,
                    "Negotiation closed because another provider was selected.");

            notificationService.createNotification(
                    negotiation.getProvider().getId(),
                    NotificationType.NEGOTIATION,
                    "Negotiation Closed",
                    "Another provider was selected for this request.");
        }
    }


    //helper
    private void closeOtherInterests(
            Negotiation acceptedNegotiation) {

        List<RequestInterest> interests =
                requestInterestRepository.findByServiceRequest(
                        acceptedNegotiation.getServiceRequest());

        for (RequestInterest interest : interests) {

            if (interest.getProvider().getId().equals(
                    acceptedNegotiation.getProvider().getId())) {

                interest.setStatus(
                        InterestStatus.FINALIZED);
            }
            else {

                interest.setStatus(
                        InterestStatus.CANCELLED);
            }

            requestInterestRepository.save(
                    interest);
        }
    }

    private ChatMessageDto mapToChatDto(
            NegotiationMessage message) {

        return ChatMessageDto.builder()

                .negotiationId(
                        message.getNegotiation().getId())

                .messageId(
                        message.getId())

                .senderId(
                        message.getSender().getId())

                .senderName(
                        message.getSender().getName())

                .senderImage(
                        message.getSender().getImage())

                .messageType(
                        message.getMessageType())

                .message(
                        message.getMessage())

                .attachmentUrl(
                        message.getAttachmentUrl())

                .createdAt(
                        message.getCreatedAt())

                .build();
    }

}