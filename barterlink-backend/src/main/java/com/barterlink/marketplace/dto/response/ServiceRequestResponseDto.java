package com.barterlink.marketplace.dto.response;

import com.barterlink.marketplace.enums.ServiceRequestStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ServiceRequestResponseDto {

    private UUID requestId;

    private UUID requesterId;

    private String requesterName;

    private UUID selectedProviderId;

    private String selectedProviderName;

    private String title;

    private String description;

    private Integer estimatedHours;

    private Integer tokenCost;

    private Boolean selectiveRequest;

    private ServiceRequestStatus status;

    private Instant createdAt;
}