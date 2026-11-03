package com.barterlink.negotiation.repository;

import com.barterlink.negotiation.entity.Negotiation;
import com.barterlink.negotiation.entity.NegotiationMessage;
import com.barterlink.negotiation.enums.NegotiationMessageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NegotiationMessageRepository
        extends JpaRepository<NegotiationMessage, UUID> {

    List<NegotiationMessage> findByNegotiationOrderByCreatedAtAsc(
            Negotiation negotiation);

    List<NegotiationMessage> findByNegotiationAndMessageType(
            Negotiation negotiation,
            NegotiationMessageType messageType);
}