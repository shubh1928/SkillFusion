package com.barterlink.agreements.controller;

import com.barterlink.agreements.dto.request.CreateAgreementVersionRequestDto;
import com.barterlink.agreements.dto.request.SubmitAgreementVersionRequestDto;
import com.barterlink.agreements.dto.response.AgreementVersionResponseDto;
import com.barterlink.agreements.service.AgreementVersionService;
import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/negotiations/{negotiationId}/versions")
@RequiredArgsConstructor
public class AgreementVersionController {

    private final AgreementVersionService agreementVersionService;
    private final CurrentUserUtil currentUserUtil;

    @PostMapping
    public ApiResponse<AgreementVersionResponseDto> createDraft(
            @PathVariable UUID negotiationId,
            @Valid @RequestBody CreateAgreementVersionRequestDto request) {

        return ApiResponse.success(
                "Agreement version draft created",
                agreementVersionService.createDraft(
                        negotiationId,
                        currentUserUtil.getCurrentUserId(),
                        request));
    }

    @GetMapping
    public ApiResponse<List<AgreementVersionResponseDto>> getVersions(
            @PathVariable UUID negotiationId) {

        return ApiResponse.success(
                agreementVersionService.getVersions(
                        negotiationId,
                        currentUserUtil.getCurrentUserId()));
    }

    @PostMapping("/{versionId}/submit")
    public ApiResponse<AgreementVersionResponseDto> submitVersion(
            @PathVariable UUID negotiationId,
            @PathVariable UUID versionId,
            @Valid @RequestBody SubmitAgreementVersionRequestDto request) {

        return ApiResponse.success(
                "Agreement version submitted for acceptance",
                agreementVersionService.submitVersion(
                        negotiationId,
                        versionId,
                        currentUserUtil.getCurrentUserId(),
                        request));
    }

    @PostMapping("/{versionId}/accept")
    public ApiResponse<AgreementVersionResponseDto> acceptVersion(
            @PathVariable UUID negotiationId,
            @PathVariable UUID versionId) {

        return ApiResponse.success(
                "Agreement version acceptance recorded",
                agreementVersionService.acceptVersion(
                        negotiationId,
                        versionId,
                        currentUserUtil.getCurrentUserId()));
    }

    @PostMapping("/{versionId}/reject")
    public ApiResponse<AgreementVersionResponseDto> rejectVersion(
            @PathVariable UUID negotiationId,
            @PathVariable UUID versionId) {

        return ApiResponse.success(
                "Agreement version rejected",
                agreementVersionService.rejectVersion(
                        negotiationId,
                        versionId,
                        currentUserUtil.getCurrentUserId()));
    }
}