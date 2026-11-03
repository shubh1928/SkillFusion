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

    @GetMapping("/user/{userId}")
    public ApiResponse<List<RatingResponseDto>> getUserRatings(
            @PathVariable UUID userId) {

        return ApiResponse.success(
                ratingService.getUserRatings(userId));
    }

    @GetMapping("/user/{userId}/reputation")
    public ApiResponse<Double> getReputation(
            @PathVariable UUID userId) {

        return ApiResponse.success(
                ratingService.calculateReputation(userId));
    }

    @GetMapping("/session/{sessionId}/status")
    public ApiResponse<Boolean> hasCurrentUserRated(
            @PathVariable UUID sessionId
    ) {

        return ApiResponse.success(

                ratingService.hasCurrentUserRated(
                        currentUserUtil.getCurrentUserId(),
                        sessionId
                )

        );
    }
}