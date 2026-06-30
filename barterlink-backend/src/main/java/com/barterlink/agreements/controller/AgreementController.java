package com.barterlink.agreements.controller;

import com.barterlink.agreements.dto.response.AgreementResponseDto;
import com.barterlink.agreements.service.AgreementService;
import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/agreements")
@RequiredArgsConstructor
public class AgreementController {

    private final AgreementService agreementService;
    private final CurrentUserUtil currentUserUtil;

    @GetMapping("/{agreementId}")
    public ApiResponse<AgreementResponseDto> getAgreement(
            @PathVariable UUID agreementId) {

        return ApiResponse.success(
                agreementService.getAgreement(
                        agreementId));
    }

    @GetMapping("/requester")
    public ApiResponse<List<AgreementResponseDto>> getRequesterAgreements() {

        return ApiResponse.success(
                agreementService.getRequesterAgreements(
                        currentUserUtil.getCurrentUserId()));
    }

    @GetMapping("/provider")
    public ApiResponse<List<AgreementResponseDto>> getProviderAgreements() {

        return ApiResponse.success(
                agreementService.getProviderAgreements(
                        currentUserUtil.getCurrentUserId()));
    }

}