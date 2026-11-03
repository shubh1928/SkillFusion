package com.barterlink.agreements.dto.response;

import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.marketplace.enums.ExchangeMode;
import lombok.Getter;
import lombok.Setter;
import com.barterlink.agreements.enums.AgreementReleaseMode;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class AgreementResponseDto {

    private UUID agreementId;

    private UUID negotiationId;

    private UUID requestId;

    private UUID requesterId;

    private String requesterName;

    private UUID providerId;

    private String providerName;

    private Integer escrowLockedTokens;

    private AgreementStatus status;

    private Instant acceptedAt;

    private Instant completedAt;

    private String offerTitle;

    private String offerDescription;

    private String requestTitle;

    private String requestDescription;

    private ExchangeMode exchangeMode;

    private Integer estimatedHours;

    private Integer agreedTokenAmount;

    private AgreementReleaseMode releaseMode;
}
