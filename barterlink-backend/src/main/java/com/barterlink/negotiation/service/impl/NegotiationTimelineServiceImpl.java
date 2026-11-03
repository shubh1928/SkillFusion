package com.barterlink.negotiation.service.impl;

import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.negotiation.entity.Negotiation;
import com.barterlink.negotiation.entity.NegotiationMessage;
import com.barterlink.negotiation.enums.NegotiationMessageType;
import com.barterlink.negotiation.repository.NegotiationMessageRepository;
import com.barterlink.negotiation.repository.NegotiationRepository;
import com.barterlink.negotiation.service.NegotiationTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NegotiationTimelineServiceImpl
        implements NegotiationTimelineService {

    private final NegotiationRepository negotiationRepository;
    private final NegotiationMessageRepository negotiationMessageRepository;

    @Override
    public void createSystemMessage(
            UUID negotiationId,
            String message) {

        Negotiation negotiation =
                negotiationRepository.findById(negotiationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Negotiation not found"));

        createSystemMessage(
                negotiation,
                message);
    }

    @Override
    public void createSystemMessage(
            Negotiation negotiation,
            String message) {

        NegotiationMessage systemMessage =
                NegotiationMessage.builder()
                        .negotiation(negotiation)
                        .message(message)
                        .messageType(
                                NegotiationMessageType.SYSTEM)
                        .systemGenerated(true)
                        .build();

        negotiationMessageRepository.save(
                systemMessage);
    }
}