package com.barterlink.rewards.controller;

import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import com.barterlink.rewards.dto.response.ReferralCodeResponseDto;
import com.barterlink.rewards.service.ReferralAttributionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/referrals")
@RequiredArgsConstructor
public class ReferralController {

    private final CurrentUserUtil currentUserUtil;
    private final ReferralAttributionService referralAttributionService;

    @GetMapping("/me/code")
    public ApiResponse<ReferralCodeResponseDto> getMyReferralCode() {

        String code = referralAttributionService.getOrCreateReferralCode(
                currentUserUtil.getCurrentUserId()
        );

        return ApiResponse.success(
                "Referral code retrieved successfully",
                new ReferralCodeResponseDto(code)
        );
    }
}