package com.barterlink.agreements.dto.response;

import com.barterlink.agreements.enums.AgreementReleaseMode;
import com.barterlink.agreements.enums.AgreementVersionStatus;
import com.barterlink.marketplace.enums.ExchangeMode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class AgreementVersionResponseDto {

    private UUID versionId;
    private UUID negotiationId;
    private Integer versionNumber;

    private AgreementVersionStatus status;
    private ExchangeMode exchangeMode;
    private AgreementReleaseMode releaseMode;

    private String offerTitle;
    private String offerDescription;
    private String requestTitle;
    private String requestDescription;

    private String agreedTerms;

    private Integer estimatedHours;
    private Integer agreedTokenAmount;
    private Instant agreedDeadline;

    private UUID createdById;
    private UUID submittedById;
    private String changeSummary;

    private Instant createdAt;
    private Instant submittedAt;
    private Instant requesterAcceptedAt;
    private Instant providerAcceptedAt;
    private Instant activatedAt;
    private Instant supersededAt;

    private String activationFailureReason;

    private List<AgreementVersionMilestoneResponseDto> milestones =
            new ArrayList<>();
}