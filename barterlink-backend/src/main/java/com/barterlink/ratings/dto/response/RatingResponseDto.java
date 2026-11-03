package com.barterlink.ratings.dto.response;

import com.barterlink.ratings.enums.RatingType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class RatingResponseDto {

    private UUID ratingId;

    private UUID sessionId;

    private UUID ratedById;

    private String ratedByName;

    private UUID ratedUserId;

    private String ratedUserName;

    private RatingType ratingType;

    private Integer stars;

    private String review;

    private Instant createdAt;
}