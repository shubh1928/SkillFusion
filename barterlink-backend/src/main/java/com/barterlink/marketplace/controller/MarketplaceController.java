package com.barterlink.marketplace.controller;

import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import com.barterlink.marketplace.dto.request.CreateServiceRequestDto;
import com.barterlink.marketplace.dto.response.ServiceRequestResponseDto;
import com.barterlink.marketplace.service.MarketplaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.barterlink.auth.services.UserActivityService;
import com.barterlink.rewards.enums.RewardQualifyingActivityType;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/marketplace")
@RequiredArgsConstructor
public class MarketplaceController {

    private final MarketplaceService marketplaceService;
    private final CurrentUserUtil currentUserUtil;
    private final UserActivityService userActivityService;


    @PostMapping("/requests")
    public ApiResponse<ServiceRequestResponseDto> createRequest(
            @Valid @RequestBody CreateServiceRequestDto dto) {

        ServiceRequestResponseDto createdRequest =
                marketplaceService.createRequest(
                        currentUserUtil.getCurrentUserId(),
                        dto
                );

        return ApiResponse.success(
                "Request created successfully",
                createdRequest
        );
    }


    @GetMapping("/requests")
    public ApiResponse<List<ServiceRequestResponseDto>> getOpenRequests() {

        return ApiResponse.success(
                marketplaceService.getAllOpenRequests());
    }

    @GetMapping("/requests/{requestId}")
    public ApiResponse<ServiceRequestResponseDto> getRequest(
            @PathVariable UUID requestId) {

        return ApiResponse.success(
                marketplaceService.getRequestById(requestId));
    }

    @GetMapping("/requests/my")
    public ApiResponse<List<ServiceRequestResponseDto>> getMyRequests() {

        return ApiResponse.success(
                marketplaceService.getMyRequests(
                        currentUserUtil.getCurrentUserId()));
    }



    @PostMapping("/requests/{requestId}/cancel")
    public ApiResponse<Void> cancelRequest(
            @PathVariable UUID requestId) {

        marketplaceService.cancelRequest(
                requestId,
                currentUserUtil.getCurrentUserId()
        );

        // Record activity only after cancellation succeeds.
        userActivityService.recordCurrentUserActivity();

        return ApiResponse.success(
                "Request cancelled successfully"
        );
    }

}