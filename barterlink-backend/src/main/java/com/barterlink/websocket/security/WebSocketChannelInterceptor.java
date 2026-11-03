package com.barterlink.websocket.security;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.auth.services.impl.JwtService;
import com.barterlink.collaboration.entity.CollaborationRoom;
import com.barterlink.collaboration.enums.CollaborationRoomStatus;
import com.barterlink.collaboration.repository.CollaborationRoomRepository;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.enums.SessionMode;
import com.barterlink.sessions.service.SessionService;
import com.barterlink.negotiation.entity.Negotiation;
import com.barterlink.negotiation.repository.NegotiationRepository;
import com.barterlink.sessions.repository.ServiceSessionRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.barterlink.common.exception.BusinessException;
import org.springframework.context.annotation.Lazy;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import com.barterlink.agreements.enums.AgreementVersionStatus;
import com.barterlink.agreements.repository.AgreementVersionRepository;

@Component
public class WebSocketChannelInterceptor
        implements ChannelInterceptor {

    private static final String COLLABORATION_TOPIC_PREFIX =
            "/topic/collaboration/";

    private static final String SESSION_TOPIC_PREFIX =
            "/topic/session/";

    private static final String NEGOTIATION_TOPIC_PREFIX =
            "/topic/negotiation/";

    private static final String AGREEMENT_TOPIC_PREFIX =
            "/topic/agreement/";

    private static final String AGREEMENT_DRAFT_TOPIC_PREFIX =
            "/topic/agreement-draft/";

    private static final String NOTIFICATION_TOPIC_PREFIX =
            "/topic/notifications/";

    private static final String TOPIC_PREFIX =
            "/topic/";

    private static final String APPLICATION_DESTINATION_PREFIX =
            "/app/";

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final CollaborationRoomRepository collaborationRoomRepository;
    private final SessionService sessionService;
    private final ServiceSessionRepository serviceSessionRepository;
    private final NegotiationRepository negotiationRepository;

    private final AgreementVersionRepository agreementVersionRepository;

    public WebSocketChannelInterceptor(
            JwtService jwtService,
            UserRepository userRepository,
            CollaborationRoomRepository collaborationRoomRepository,
            @Lazy SessionService sessionService,
            ServiceSessionRepository serviceSessionRepository,
            NegotiationRepository negotiationRepository,
            AgreementVersionRepository agreementVersionRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.collaborationRoomRepository = collaborationRoomRepository;
        this.sessionService = sessionService;
        this.serviceSessionRepository = serviceSessionRepository;
        this.negotiationRepository = negotiationRepository;
        this.agreementVersionRepository = agreementVersionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Message<?> preSend(
            Message<?> message,
            MessageChannel channel) {

        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(
                        message,
                        StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            authenticateConnect(accessor);
            return message;
        }

        if (StompCommand.SEND.equals(command)) {

            requireAuthenticatedUser(accessor.getUser());

            String destination = accessor.getDestination();

            /*
             * Clients must send messages through application
             * handlers, not publish directly to broker topics.
             */
            if (destination == null
                    || !destination.startsWith(
                    APPLICATION_DESTINATION_PREFIX)) {

                throw new AccessDeniedException(
                        "Client messages must use an application destination");
            }
        }

        if (StompCommand.SUBSCRIBE.equals(command)) {

            Principal principal =
                    requireAuthenticatedUser(accessor.getUser());

            String destination = accessor.getDestination();

            if (destination == null
                    || !destination.startsWith(TOPIC_PREFIX)) {

                throw new AccessDeniedException(
                        "WebSocket subscription destination is not authorized");
            }

            if (destination.startsWith(COLLABORATION_TOPIC_PREFIX)) {

                authorizeCollaborationSubscription(
                        destination,
                        principal);

            } else if (destination.startsWith(SESSION_TOPIC_PREFIX)) {

                authorizeSessionSubscription(
                        destination,
                        principal);

            } else if (destination.startsWith(NEGOTIATION_TOPIC_PREFIX)) {

                authorizeNegotiationSubscription(
                        destination,
                        principal);


            } else if (destination.startsWith(AGREEMENT_DRAFT_TOPIC_PREFIX)) {

                authorizeAgreementDraftSubscription(
                        destination,
                        principal
                );


            } else if (destination.startsWith(AGREEMENT_TOPIC_PREFIX)) {

                authorizeAgreementSubscription(
                        destination,
                        principal);

            } else if (destination.startsWith(NOTIFICATION_TOPIC_PREFIX)) {

                authorizeNotificationSubscription(
                        destination,
                        principal);

            } else {

                throw new AccessDeniedException(
                        "WebSocket subscription destination is not authorized");
            }
        }

        return message;
    }

    private void authenticateConnect(
            StompHeaderAccessor accessor) {

        List<String> authorizationHeaders =
                accessor.getNativeHeader("Authorization");

        if (authorizationHeaders == null
                || authorizationHeaders.isEmpty()
                || authorizationHeaders.get(0) == null
                || !authorizationHeaders.get(0).startsWith("Bearer ")) {

            throw new AccessDeniedException(
                    "A valid WebSocket access token is required");
        }

        String token =
                authorizationHeaders.get(0)
                        .substring(7)
                        .trim();

        if (token.isBlank()) {
            throw new AccessDeniedException(
                    "A valid WebSocket access token is required");
        }

        try {

            if (!jwtService.isAccessToken(token)) {
                throw new AccessDeniedException(
                        "Invalid WebSocket access token");
            }

            UUID userId =
                    jwtService.getUserId(token);

            User user =
                    userRepository.findById(userId)
                            .orElse(null);

            if (user == null || !user.isEnabled()) {
                throw new AccessDeniedException(
                        "WebSocket access is not authorized");
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            user.getId().toString(),
                            null,
                            List.of());

            accessor.setUser(authentication);

        } catch (AccessDeniedException exception) {

            throw exception;

        } catch (RuntimeException exception) {

            throw new AccessDeniedException(
                    "WebSocket authentication failed",
                    exception);
        }
    }

    private Principal requireAuthenticatedUser(
            Principal principal) {

        if (principal == null
                || principal.getName() == null
                || principal.getName().isBlank()) {

            throw new AccessDeniedException(
                    "WebSocket authentication is required");
        }

        return principal;
    }

    private void authorizeCollaborationSubscription(
            String destination,
            Principal principal) {

        String roomIdValue =
                destination.substring(
                        COLLABORATION_TOPIC_PREFIX.length());

        if (roomIdValue.isBlank()
                || roomIdValue.contains("/")) {

            throw new AccessDeniedException(
                    "Collaboration subscription is not authorized");
        }

        UUID roomId;

        try {

            roomId = UUID.fromString(roomIdValue);

        } catch (IllegalArgumentException exception) {

            throw new AccessDeniedException(
                    "Collaboration subscription is not authorized",
                    exception);
        }

        CollaborationRoom room =
                collaborationRoomRepository.findById(roomId)
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Collaboration subscription is not authorized"));

        if (room.getStatus()
                != CollaborationRoomStatus.ACTIVE) {

            throw new AccessDeniedException(
                    "Collaboration room is not active");
        }

        UUID currentUserId;

        try {

            currentUserId =
                    UUID.fromString(principal.getName());

        } catch (IllegalArgumentException exception) {

            throw new AccessDeniedException(
                    "Collaboration subscription is not authorized",
                    exception);
        }

        ServiceSession session = room.getSession();

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

            throw new AccessDeniedException(
                    "You are not authorized to access this collaboration room");
        }

        /*
         * Temporary fail-closed gate:
         * live signaling and subscriptions remain unavailable
         * until the persisted recording-consent and recorder-
         * readiness workflow has been implemented and verified.
         */
        if (session.getAgreedSessionMode()
                != SessionMode.LIVE_VIDEO_SESSION) {

            throw new AccessDeniedException(
                    "Live collaboration is not authorized for this session");
        }

        try {
            sessionService.assertLiveMediaReady(
                    session.getId(),
                    currentUserId
            );
        } catch (BusinessException exception) {
            throw new AccessDeniedException(
                    "Live collaboration is not authorized for this session",
                    exception
            );
        }
    }

    private void authorizeSessionSubscription(
            String destination,
            Principal principal) {

        UUID sessionId = parseSubscriptionId(
                destination,
                SESSION_TOPIC_PREFIX);

        ServiceSession session =
                serviceSessionRepository.findById(sessionId)
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Session subscription is not authorized"));

        UUID currentUserId = parsePrincipalUserId(principal);

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

            throw new AccessDeniedException(
                    "You are not authorized to access this session");
        }
    }


    private void authorizeNegotiationSubscription(
            String destination,
            Principal principal) {

        UUID negotiationId = parseSubscriptionId(
                destination,
                NEGOTIATION_TOPIC_PREFIX);

        Negotiation negotiation =
                negotiationRepository.findById(negotiationId)
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Negotiation subscription is not authorized"));

        UUID currentUserId = parsePrincipalUserId(principal);

        UUID requesterId =
                negotiation.getRequester().getId();

        UUID providerId =
                negotiation.getProvider().getId();

        if (!requesterId.equals(currentUserId)
                && !providerId.equals(currentUserId)) {

            throw new AccessDeniedException(
                    "You are not authorized to access this negotiation");
        }
    }


    private void authorizeAgreementDraftSubscription(
            String destination,
            Principal principal) {

        UUID negotiationId = parseSubscriptionId(
                destination,
                AGREEMENT_DRAFT_TOPIC_PREFIX
        );

        Negotiation negotiation =
                negotiationRepository.findById(negotiationId)
                        .orElseThrow(() ->
                                new AccessDeniedException(
                                        "Agreement draft subscription is not authorized"
                                )
                        );

        UUID currentUserId = parsePrincipalUserId(principal);

        UUID requesterId =
                negotiation.getRequester().getId();

        UUID providerId =
                negotiation.getProvider().getId();

        if (!requesterId.equals(currentUserId)
                && !providerId.equals(currentUserId)) {

            throw new AccessDeniedException(
                    "You are not authorized to access this shared agreement draft"
            );
        }
    }



    private void authorizeAgreementSubscription(
            String destination,
            Principal principal) {

        String route = destination.substring(
                AGREEMENT_TOPIC_PREFIX.length());

        String[] parts = route.split("/", -1);

        if (parts.length != 2
                || parts[0].isBlank()
                || parts[1].isBlank()) {

            throw new AccessDeniedException(
                    "Agreement subscription is not authorized");
        }

        final UUID negotiationId;
        final UUID versionId;

        try {
            negotiationId = UUID.fromString(parts[0]);
            versionId = UUID.fromString(parts[1]);
        } catch (IllegalArgumentException exception) {
            throw new AccessDeniedException(
                    "Agreement subscription is not authorized",
                    exception);
        }

        var version = agreementVersionRepository.findById(versionId)
                .orElseThrow(() ->
                        new AccessDeniedException(
                                "Agreement subscription is not authorized"));

        if (!version.getNegotiation().getId().equals(negotiationId)) {
            throw new AccessDeniedException(
                    "Agreement does not belong to this negotiation");
        }

        if (version.getStatus() != AgreementVersionStatus.DRAFT) {
            throw new AccessDeniedException(
                    "Only draft agreements allow collaborative editing");
        }

        UUID currentUserId = parsePrincipalUserId(principal);

        UUID requesterId =
                version.getNegotiation().getRequester().getId();

        UUID providerId =
                version.getNegotiation().getProvider().getId();

        if (!requesterId.equals(currentUserId)
                && !providerId.equals(currentUserId)) {

            throw new AccessDeniedException(
                    "You are not authorized to access this agreement");
        }
    }



    private void authorizeNotificationSubscription(
            String destination,
            Principal principal) {

        UUID notificationUserId = parseSubscriptionId(
                destination,
                NOTIFICATION_TOPIC_PREFIX);

        UUID currentUserId = parsePrincipalUserId(principal);

        if (!notificationUserId.equals(currentUserId)) {

            throw new AccessDeniedException(
                    "You are not authorized to access these notifications");
        }
    }


    private UUID parseSubscriptionId(
            String destination,
            String prefix) {

        if (destination == null
                || !destination.startsWith(prefix)) {

            throw new AccessDeniedException(
                    "WebSocket subscription is not authorized");
        }

        String idValue =
                destination.substring(prefix.length());

        if (idValue.isBlank() || idValue.contains("/")) {

            throw new AccessDeniedException(
                    "WebSocket subscription is not authorized");
        }

        try {

            UUID parsedId = UUID.fromString(idValue);

            if (!parsedId.toString().equalsIgnoreCase(idValue)) {

                throw new AccessDeniedException(
                        "WebSocket subscription is not authorized");
            }

            return parsedId;

        } catch (IllegalArgumentException exception) {

            throw new AccessDeniedException(
                    "WebSocket subscription is not authorized",
                    exception);
        }
    }


    private UUID parsePrincipalUserId(
            Principal principal) {

        try {

            return UUID.fromString(principal.getName());

        } catch (IllegalArgumentException
                 | NullPointerException exception) {

            throw new AccessDeniedException(
                    "WebSocket user identity is invalid",
                    exception);
        }
    }

}