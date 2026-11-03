package com.barterlink.marketplace.dto.response;

import com.barterlink.ai.dto.TrustAssessmentSummaryDto;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.marketplace.enums.ServiceRequestStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class ServiceRequestResponseDto {

    private UUID requestId;

    private UUID requesterId;

    private String requesterName;

    private String title;

    private Integer estimatedHours;

    private ExchangeMode exchangeMode;

    private String offerTitle;

    private String offerDescription;

    private String requestTitle;

    private String requestDescription;

    private Integer tokenAmount;

    private Boolean requesterHasSufficientTokens;

    private Boolean selectiveRequest;

    private ServiceRequestStatus status;

    private Instant createdAt;

    private List<String> requiredSkills;

    private TrustAssessmentSummaryDto trustAssessment;
}