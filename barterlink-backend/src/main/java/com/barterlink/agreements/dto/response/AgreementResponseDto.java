package com.barterlink.agreements.dto.response;

import com.barterlink.agreements.enums.AgreementStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class AgreementResponseDto {

    private UUID agreementId;

    private String requestTitle;

    private UUID requestId;

    private UUID requesterId;

    private String requesterName;

    private UUID providerId;

    private String providerName;

    private Integer escrowLockedTokens;

    private AgreementStatus status;

    private Instant acceptedAt;

    private Instant completedAt;
}
