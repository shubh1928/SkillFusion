package com.barterlink.collaboration.signaling.service.impl;

import com.barterlink.collaboration.repository.CollaborationRoomRepository;
import com.barterlink.collaboration.signaling.dto.CollaborationSignalMessageDto;
import com.barterlink.collaboration.signaling.service.SignalingService;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.websocket.constants.WebSocketDestinationConstants;
import com.barterlink.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.barterlink.collaboration.entity.CollaborationRoom;
import com.barterlink.collaboration.enums.CollaborationRoomStatus;
import com.barterlink.common.exception.BusinessException;
import java.security.Principal;
import org.springframework.transaction.annotation.Transactional;
import com.barterlink.sessions.enums.SessionMode;
import com.barterlink.sessions.service.SessionService;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SignalingServiceImpl
        implements SignalingService {

    private final WebSocketService webSocketService;
    private final CollaborationRoomRepository collaborationRoomRepository;
    private final SessionService sessionService;

    @Override
    @Transactional
    public void relaySignal(
            CollaborationSignalMessageDto signal,
            Principal principal) {

        validateSignal(
                signal,
                principal);

        UUID currentUserId =
                UUID.fromString(principal.getName());

        signal.setSenderId(currentUserId);

        switch (signal.getSignalType()) {

            case JOIN -> handleJoin(signal);

            case READY -> handleReady(signal);

            case LEAVE -> handleLeave(signal);

            case OFFER -> handleOffer(signal);

            case ANSWER -> handleAnswer(signal);

            case ICE_CANDIDATE -> handleIceCandidate(signal);

        }
    }

    private void validateSignal(
            CollaborationSignalMessageDto signal,
            Principal principal) {

        UUID currentUserId =
                UUID.fromString(
                        principal.getName());

        CollaborationRoom room =
                collaborationRoomRepository
                        .findById(signal.getRoomId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Collaboration room not found"));

        if (room.getStatus() != CollaborationRoomStatus.ACTIVE) {

            throw new BusinessException(
                    "Collaboration room is not active");
        }

        UUID requesterId =
                room.getSession()
                        .getAgreement()
                        .getRequester()
                        .getId();

        UUID providerId =
                room.getSession()
                        .getAgreement()
                        .getProvider()
                        .getId();

        if (!requesterId.equals(currentUserId)
                && !providerId.equals(currentUserId)) {

            throw new BusinessException(
                    "Access denied");
        }

        UUID receiverId =
                signal.getReceiverId();

        if (!receiverId.equals(requesterId)
                && !receiverId.equals(providerId)) {

            throw new BusinessException(
                    "Invalid receiver");
        }

        if (receiverId.equals(currentUserId)) {

            throw new BusinessException(
                    "You cannot send signaling messages to yourself");
        }

        boolean isLeaveSignal =
                signal.getSignalType() != null
                        && "LEAVE".equals(
                        signal.getSignalType().name());

        if (!isLeaveSignal) {

            if (room.getSession().getAgreedSessionMode()
                    != SessionMode.LIVE_VIDEO_SESSION) {

                throw new BusinessException(
                        "Live media signaling is disabled unless both participants have agreed to live video.");
            }

            sessionService.assertLiveMediaReady(
                    room.getSession().getId(),
                    currentUserId
            );
        }

    }

    private void handleJoin(
            CollaborationSignalMessageDto signal) {

        System.out.println("BROADCASTING JOIN");
        System.out.println(
                WebSocketDestinationConstants.COLLABORATION_TOPIC
                        + signal.getRoomId()
        );

        webSocketService.sendCollaborationSignal(

                WebSocketDestinationConstants.COLLABORATION_TOPIC
                        + signal.getRoomId(),

                signal
        );
    }

    private void handleReady(
            CollaborationSignalMessageDto signal) {

        webSocketService.sendCollaborationSignal(

                WebSocketDestinationConstants.COLLABORATION_TOPIC
                        + signal.getRoomId(),

                signal
        );

    }


    private void handleLeave(
            CollaborationSignalMessageDto signal) {

        webSocketService.sendCollaborationSignal(

                WebSocketDestinationConstants.COLLABORATION_TOPIC
                        + signal.getRoomId(),

                signal
        );

    }

    private void handleOffer(
            CollaborationSignalMessageDto signal) {

        webSocketService.sendCollaborationSignal(

                WebSocketDestinationConstants.COLLABORATION_TOPIC
                        + signal.getRoomId(),

                signal
        );

    }

    private void handleAnswer(
            CollaborationSignalMessageDto signal) {

        webSocketService.sendCollaborationSignal(

                WebSocketDestinationConstants.COLLABORATION_TOPIC
                        + signal.getRoomId(),

                signal
        );

    }

    private void handleIceCandidate(
            CollaborationSignalMessageDto signal) {

        webSocketService.sendCollaborationSignal(

                WebSocketDestinationConstants.COLLABORATION_TOPIC
                        + signal.getRoomId(),

                signal
        );

    }

}