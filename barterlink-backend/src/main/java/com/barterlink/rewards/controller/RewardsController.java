package com.barterlink.rewards.controller;

import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import com.barterlink.rewards.dto.response.RewardsOverviewResponseDto;
import com.barterlink.rewards.service.RewardsOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rewards")
@RequiredArgsConstructor
public class RewardsController {

    private final CurrentUserUtil currentUserUtil;
    private final RewardsOverviewService rewardsOverviewService;

    @GetMapping("/me/overview")
    public ApiResponse<RewardsOverviewResponseDto> getMyRewardsOverview() {
        return ApiResponse.success(
                "Reward progress retrieved successfully",
                rewardsOverviewService.getOverview(
                        currentUserUtil.getCurrentUserId()
                )
        );
    }
}