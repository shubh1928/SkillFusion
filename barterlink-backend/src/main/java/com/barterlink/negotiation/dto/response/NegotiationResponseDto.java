package com.barterlink.negotiation.dto.response;

import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.negotiation.enums.NegotiationStatus;
import lombok.Getter;
import lombok.Setter;
import com.barterlink.agreements.enums.AgreementReleaseMode;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class NegotiationResponseDto {

    private UUID negotiationId;

    private UUID requestId;

    private UUID requesterId;

    private UUID providerId;

    private NegotiationStatus status;

    private String offerTitle;

    private String offerDescription;

    private String requestTitle;

    private String requestDescription;

    private ExchangeMode exchangeMode;

    private Integer estimatedHours;

    private Integer tokenAmount;

    private Instant createdAt;


    /*
     * Release-mode selection by each participant.
     */
    private AgreementReleaseMode requesterReleaseMode;
    private AgreementReleaseMode providerReleaseMode;

    private Integer requesterMilestoneCount;
    private Integer providerMilestoneCount;

    /*
     * True for new negotiations that use explicit
     * release-mode selection.
     */
    private Boolean releaseModeSelectionRequired;
}