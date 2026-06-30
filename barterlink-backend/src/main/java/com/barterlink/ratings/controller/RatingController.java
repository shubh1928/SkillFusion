package com.barterlink.ratings.controller;

import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import com.barterlink.ratings.dto.request.CreateRatingRequestDto;
import com.barterlink.ratings.dto.response.RatingResponseDto;
import com.barterlink.ratings.service.RatingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ratings")
@RequiredArgsConstructor
public class RatingController {

    private final RatingService ratingService;
    private final CurrentUserUtil currentUserUtil;

    @PostMapping
    public ApiResponse<RatingResponseDto> submitRating(
            @Valid @RequestBody CreateRatingRequestDto request) {

        return ApiResponse.success(
                "Rating submitted successfully",
                ratingService.submitRating(
                        currentUserUtil.getCurrentUserId(),
                        request));
    }

    @GetMapping("/provider/{providerId}")
    public ApiResponse<List<RatingResponseDto>> getProviderRatings(
            @PathVariable UUID providerId) {

        return ApiResponse.success(
                ratingService.getProviderRatings(
                        providerId));
    }

    @GetMapping("/provider/{providerId}/reputation")
    public ApiResponse<Double> getProviderReputation(
            @PathVariable UUID providerId) {

        return ApiResponse.success(
                ratingService.calculateReputation(
                        providerId));
    }
}
