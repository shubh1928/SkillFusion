package com.barterlink.negotiation.websocket.controller;

import com.barterlink.common.exception.BusinessException;
import com.barterlink.negotiation.dto.request.SendNegotiationMessageRequestDto;
import com.barterlink.negotiation.service.NegotiationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class NegotiationWebSocketController {

    private final NegotiationService negotiationService;


    @MessageMapping("/negotiation/{negotiationId}/message")
    public void sendMessage(
            @DestinationVariable UUID negotiationId,
            @Valid @Payload SendNegotiationMessageRequestDto request,
            Principal principal) {

        if (principal == null || principal.getName() == null) {

            throw new BusinessException(
                    "WebSocket user is not authenticated"
            );
        }


        final UUID senderId;

        try {

            senderId = UUID.fromString(
                    principal.getName()
            );

        } catch (IllegalArgumentException exception) {

            throw new BusinessException(
                    "Invalid WebSocket user identity"
            );
        }


        negotiationService.sendMessage(
                negotiationId,
                senderId,
                request
        );
    }

}