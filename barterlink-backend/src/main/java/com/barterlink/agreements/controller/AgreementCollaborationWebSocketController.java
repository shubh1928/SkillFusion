
package com.barterlink.agreements.controller;

import com.barterlink.agreements.dto.request.AgreementCollaborationUpdateRequestDto;
import com.barterlink.agreements.service.AgreementCollaborationUpdateService;
import com.barterlink.common.exception.BusinessException;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class AgreementCollaborationWebSocketController {

    private final AgreementCollaborationUpdateService collaborationUpdateService;

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/agreement/{negotiationId}/{versionId}/update")
    public void handleUpdate(
            @DestinationVariable UUID negotiationId,
            @DestinationVariable UUID versionId,
            @Valid @Payload AgreementCollaborationUpdateRequestDto request,
            Principal principal) {

        if (principal == null
                || principal.getName() == null) {

            throw new BusinessException(
                    "WebSocket user is not authenticated");
        }

        final UUID userId;

        try {
            userId = UUID.fromString(principal.getName());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    "Invalid WebSocket user identity");
        }

        collaborationUpdateService.saveUpdate(
                negotiationId,
                versionId,
                userId,
                request
        );

        String topic =
                "/topic/agreement/"
                        + negotiationId
                        + "/"
                        + versionId;

        /*
         * Broadcast only after the database save succeeds.
         * Both participants can receive the update.
         */
        messagingTemplate.convertAndSend(topic, request);
    }


    @MessageMapping("/agreement-draft/{negotiationId}/update")
    public void handleNegotiationDraftUpdate(
            @DestinationVariable UUID negotiationId,
            @Valid @Payload AgreementCollaborationUpdateRequestDto request,
            Principal principal) {

        if (principal == null || principal.getName() == null) {
            throw new BusinessException(
                    "WebSocket user is not authenticated"
            );
        }

        final UUID userId;

        try {
            userId = UUID.fromString(principal.getName());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    "Invalid WebSocket user identity"
            );
        }

        collaborationUpdateService.saveNegotiationUpdate(
                negotiationId,
                userId,
                request
        );

        String topic = "/topic/agreement-draft/" + negotiationId;

        /*
         * Broadcast after the database save succeeds.
         * Repeated delivery is safe for the Yjs document.
         */
        messagingTemplate.convertAndSend(topic, request);
    }


}
