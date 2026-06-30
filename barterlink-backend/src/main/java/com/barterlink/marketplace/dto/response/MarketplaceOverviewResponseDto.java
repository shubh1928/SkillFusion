package com.barterlink.marketplace.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MarketplaceOverviewResponseDto {

    private Long openRequests;

    private Long acceptedRequests;

    private Long completedRequests;

    private Long cancelledRequests;
}