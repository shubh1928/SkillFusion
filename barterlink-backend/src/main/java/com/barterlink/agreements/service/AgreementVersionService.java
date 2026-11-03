package com.barterlink.agreements.service;

import com.barterlink.agreements.dto.request.CreateAgreementVersionRequestDto;
import com.barterlink.agreements.dto.request.SubmitAgreementVersionRequestDto;
import com.barterlink.agreements.dto.response.AgreementVersionResponseDto;
import com.barterlink.agreements.entity.AgreementVersion;

import java.util.List;
import java.util.UUID;

public interface AgreementVersionService {

    AgreementVersionResponseDto createDraft(
            UUID negotiationId,
            UUID userId,
            CreateAgreementVersionRequestDto request);

    List<AgreementVersionResponseDto> getVersions(
            UUID negotiationId,
            UUID userId);

    AgreementVersionResponseDto submitVersion(
            UUID negotiationId,
            UUID versionId,
            UUID userId,
            SubmitAgreementVersionRequestDto request);

    AgreementVersionResponseDto acceptVersion(
            UUID negotiationId,
            UUID versionId,
            UUID userId);

    AgreementVersionResponseDto rejectVersion(
            UUID negotiationId,
            UUID versionId,
            UUID userId);

    AgreementVersion requireReadyInitialVersion(
            UUID negotiationId);

    void activateInitialVersion(
            AgreementVersion version,
            com.barterlink.agreements.entity.ServiceAgreement agreement);

    boolean hasAnyVersion(UUID negotiationId);
}