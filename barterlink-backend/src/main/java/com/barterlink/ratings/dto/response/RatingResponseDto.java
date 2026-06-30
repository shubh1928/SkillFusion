package com.barterlink.ratings.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class RatingResponseDto {

    private UUID ratingId;

    private UUID agreementId;

    private UUID providerId;

    private String providerName;

    private UUID reviewerId;

    private String reviewerName;

    private Integer stars;

    private String review;

    private Instant createdAt;
}