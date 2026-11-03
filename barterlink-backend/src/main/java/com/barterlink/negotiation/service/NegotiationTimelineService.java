package com.barterlink.negotiation.service;

import com.barterlink.negotiation.entity.Negotiation;

import java.util.UUID;

public interface NegotiationTimelineService {

    void createSystemMessage(
            Negotiation negotiation,
            String message);

    void createSystemMessage(
            UUID negotiationId,
            String message);
}