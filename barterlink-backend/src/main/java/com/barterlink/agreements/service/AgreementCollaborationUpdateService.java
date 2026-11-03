package com.barterlink.agreements.service;

import com.barterlink.agreements.dto.request.AgreementCollaborationUpdateRequestDto;
import com.barterlink.agreements.dto.response.AgreementCollaborationUpdateResponseDto;

import java.util.List;
import java.util.UUID;

public interface AgreementCollaborationUpdateService {

    // Existing version-level collaboration methods

    List<AgreementCollaborationUpdateResponseDto> getUpdates(
            UUID negotiationId,
            UUID versionId,
            UUID userId
    );

    boolean saveUpdate(
            UUID negotiationId,
            UUID versionId,
            UUID userId,
            AgreementCollaborationUpdateRequestDto request
    );

    AgreementCollaborationUpdateResponseDto initializeDocument(
            UUID negotiationId,
            UUID versionId,
            UUID userId,
            AgreementCollaborationUpdateRequestDto request
    );


    // New negotiation-level collaboration methods

    List<AgreementCollaborationUpdateResponseDto> getNegotiationUpdates(
            UUID negotiationId,
            UUID userId
    );

    boolean saveNegotiationUpdate(
            UUID negotiationId,
            UUID userId,
            AgreementCollaborationUpdateRequestDto request
    );

    AgreementCollaborationUpdateResponseDto initializeNegotiationDocument(
            UUID negotiationId,
            UUID userId,
            AgreementCollaborationUpdateRequestDto request
    );
}