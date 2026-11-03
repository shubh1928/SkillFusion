package com.barterlink.ratings.service;

import com.barterlink.ratings.dto.request.CreateRatingRequestDto;
import com.barterlink.ratings.dto.response.RatingResponseDto;

import java.util.List;
import java.util.UUID;

public interface RatingService {

    RatingResponseDto submitRating(
            UUID currentUserId,
            CreateRatingRequestDto request);

    List<RatingResponseDto> getUserRatings(
            UUID userId);

    Double calculateReputation(
            UUID userId);

    Boolean hasCurrentUserRated(
            UUID currentUserId,
            UUID sessionId);
}