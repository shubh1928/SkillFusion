package com.barterlink.ai.controller;

import com.barterlink.ai.service.RecommendationService;
import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import com.barterlink.marketplace.dto.response.ServiceRequestResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    private final CurrentUserUtil currentUserUtil;

    @GetMapping
    public ApiResponse<List<ServiceRequestResponseDto>> getRecommendations() {

        return ApiResponse.success(

                recommendationService.getRecommendations(
                        currentUserUtil.getCurrentUserId()
                )

        );
    }
}