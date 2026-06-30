package com.barterlink.ratings.service;

import com.barterlink.ratings.dto.request.CreateRatingRequestDto;
import com.barterlink.ratings.dto.response.RatingResponseDto;

import java.util.List;
import java.util.UUID;

public interface RatingService {

    RatingResponseDto submitRating(
            UUID reviewerId,
            CreateRatingRequestDto request);

    List<RatingResponseDto> getProviderRatings(
            UUID providerId);

    Double calculateReputation(
            UUID providerId);
}