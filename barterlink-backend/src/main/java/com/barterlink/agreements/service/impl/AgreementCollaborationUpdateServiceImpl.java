
package com.barterlink.agreements.service.impl;

import com.barterlink.agreements.dto.request.AgreementCollaborationUpdateRequestDto;
import com.barterlink.agreements.dto.response.AgreementCollaborationUpdateResponseDto;
import com.barterlink.agreements.entity.AgreementCollaborationUpdate;
import com.barterlink.agreements.entity.AgreementVersion;
import com.barterlink.agreements.enums.AgreementVersionStatus;
import com.barterlink.agreements.repository.AgreementCollaborationUpdateRepository;
import com.barterlink.agreements.repository.AgreementVersionRepository;
import com.barterlink.agreements.service.AgreementCollaborationUpdateService;
import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.negotiation.entity.Negotiation;
import com.barterlink.negotiation.repository.NegotiationRepository;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgreementCollaborationUpdateServiceImpl
        implements AgreementCollaborationUpdateService {

    private static final int MAX_UPDATE_BYTES = 1024 * 1024;
    private static final String INITIAL_UPDATE_ID = "INITIAL";
    private final NegotiationRepository negotiationRepository;
    private final AgreementVersionRepository versionRepository;
    private final AgreementCollaborationUpdateRepository updateRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AgreementCollaborationUpdateResponseDto> getUpdates(
            UUID negotiationId,
            UUID versionId,
            UUID userId) {

        AgreementVersion version = getAuthorizedDraft(
                negotiationId,
                versionId,
                userId
        );

        return updateRepository
                .findByVersion_IdOrderByCreatedAtAscIdAsc(version.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Override
    @Transactional
    public boolean saveUpdate(
            UUID negotiationId,
            UUID versionId,
            UUID userId,
            AgreementCollaborationUpdateRequestDto request) {

        if (request == null) {
            throw new BusinessException(
                    "Collaboration update is required");
        }

        AgreementVersion version = getAuthorizedDraftForUpdate(
                negotiationId,
                versionId,
                userId
        );

        if (!isValidUuid(request.getClientUpdateId())) {
            throw new BusinessException(
                    "Invalid collaboration update identifier");
        }

        String clientUpdateId = request.getClientUpdateId();

        byte[] payload;

        try {
            payload = Base64.getDecoder().decode(
                    request.getUpdatePayload());
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    "Invalid collaboration update encoding");
        }

        if (payload.length == 0 || payload.length > MAX_UPDATE_BYTES) {
            throw new BusinessException(
                    "Collaboration update must contain between 1 byte and 1 MB");
        }

        /*
         * Prevent duplicate updates and reject reuse of an
         * existing update ID with different content.
         */
        var existingUpdate =
                updateRepository.findByVersion_IdAndClientUpdateId(
                        version.getId(),
                        clientUpdateId);

        if (existingUpdate.isPresent()) {

            if (!Arrays.equals(
                    existingUpdate.get().getUpdatePayload(),
                    payload)) {

                throw new BusinessException(
                        "Collaboration update identifier was reused with different content");
            }

            return false;
        }

        User sender = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        AgreementCollaborationUpdate update =
                AgreementCollaborationUpdate.builder()
                        .version(version)
                        .sender(sender)
                        .clientUpdateId(clientUpdateId)
                        .updatePayload(payload)
                        .build();

        updateRepository.saveAndFlush(update);

        return true;
    }



    @Override
    @Transactional
    public AgreementCollaborationUpdateResponseDto initializeDocument(
            UUID negotiationId,
            UUID versionId,
            UUID userId,
            AgreementCollaborationUpdateRequestDto request) {

        if (request == null || request.getUpdatePayload() == null
                || request.getUpdatePayload().isBlank()) {

            throw new BusinessException(
                    "Initial agreement document update is required");
        }

        Negotiation negotiation = negotiationRepository.findById(negotiationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Negotiation not found"));

        if (userId == null
                || (!negotiation.getRequester().getId().equals(userId)
                && !negotiation.getProvider().getId().equals(userId))) {

            throw new BusinessException(
                    "You are not a participant in this negotiation");
        }

        /*
         * Lock the agreement version row so that two participants
         * cannot initialize the same document simultaneously.
         */
        AgreementVersion version = versionRepository.findByIdForUpdate(versionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Agreement version not found"));

        if (!version.getNegotiation().getId().equals(negotiationId)) {
            throw new BusinessException(
                    "Agreement version does not belong to this negotiation");
        }

        if (version.getStatus() != AgreementVersionStatus.DRAFT) {
            throw new BusinessException(
                    "Only draft agreements can be initialized for collaboration");
        }

        /*
         * Return the already initialized document if another
         * participant initialized it first.
         */
        var existing = updateRepository
                .findByVersion_IdAndClientUpdateId(
                        versionId,
                        INITIAL_UPDATE_ID
                );

        if (existing.isPresent()) {
            return mapToResponse(existing.get());
        }

        byte[] payload;

        try {
            payload = Base64.getDecoder().decode(
                    request.getUpdatePayload()
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    "Invalid initial collaboration update encoding");
        }

        if (payload.length == 0 || payload.length > MAX_UPDATE_BYTES) {
            throw new BusinessException(
                    "Initial document update must contain between 1 byte and 1 MB");
        }

        User sender = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        AgreementCollaborationUpdate initialUpdate =
                AgreementCollaborationUpdate.builder()
                        .version(version)
                        .sender(sender)
                        .clientUpdateId(INITIAL_UPDATE_ID)
                        .updatePayload(payload)
                        .build();

        AgreementCollaborationUpdate saved =
                updateRepository.saveAndFlush(initialUpdate);

        return mapToResponse(saved);
    }


    /*
     * Negotiation-level collaboration:
     * Used by the Current Deal form before an AgreementVersion exists.
     */

    @Override
    @Transactional(readOnly = true)
    public List<AgreementCollaborationUpdateResponseDto> getNegotiationUpdates(
            UUID negotiationId,
            UUID userId) {

        Negotiation negotiation = getAuthorizedNegotiation(
                negotiationId,
                userId
        );

        return updateRepository
                .findByNegotiation_IdOrderByCreatedAtAscIdAsc(
                        negotiation.getId()
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Override
    @Transactional
    public boolean saveNegotiationUpdate(
            UUID negotiationId,
            UUID userId,
            AgreementCollaborationUpdateRequestDto request) {

        if (request == null) {
            throw new BusinessException(
                    "Collaboration update is required"
            );
        }

        /*
         * Lock the negotiation so concurrent updates for the
         * same document are processed safely.
         */
        Negotiation negotiation = getAuthorizedNegotiationForUpdate(
                negotiationId,
                userId
        );

        if (!isValidUuid(request.getClientUpdateId())) {
            throw new BusinessException(
                    "Invalid collaboration update identifier"
            );
        }

        String clientUpdateId = request.getClientUpdateId();

        byte[] payload;

        try {
            payload = Base64.getDecoder().decode(
                    request.getUpdatePayload()
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    "Invalid collaboration update encoding"
            );
        }

        if (payload.length == 0 || payload.length > MAX_UPDATE_BYTES) {
            throw new BusinessException(
                    "Collaboration update must contain between 1 byte and 1 MB"
            );
        }

        /*
         * Prevent duplicate updates and reject reuse of an
         * existing update ID with different content.
         */
        var existingUpdate =
                updateRepository.findByNegotiation_IdAndClientUpdateId(
                        negotiation.getId(),
                        clientUpdateId
                );

        if (existingUpdate.isPresent()) {

            if (!Arrays.equals(
                    existingUpdate.get().getUpdatePayload(),
                    payload
            )) {
                throw new BusinessException(
                        "Collaboration update identifier was reused with different content"
                );
            }

            return false;
        }

        User sender = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        AgreementCollaborationUpdate update =
                AgreementCollaborationUpdate.builder()
                        .negotiation(negotiation)
                        .sender(sender)
                        .clientUpdateId(clientUpdateId)
                        .updatePayload(payload)
                        .build();

        updateRepository.saveAndFlush(update);

        return true;
    }


    @Override
    @Transactional
    public AgreementCollaborationUpdateResponseDto initializeNegotiationDocument(
            UUID negotiationId,
            UUID userId,
            AgreementCollaborationUpdateRequestDto request) {

        if (request == null
                || request.getUpdatePayload() == null
                || request.getUpdatePayload().isBlank()) {

            throw new BusinessException(
                    "Initial agreement document update is required"
            );
        }

        /*
         * Lock the negotiation so both participants cannot
         * initialize separate initial documents simultaneously.
         */
        Negotiation negotiation = getAuthorizedNegotiationForUpdate(
                negotiationId,
                userId
        );

        /*
         * Return the already initialized document if one participant
         * initialized it first.
         */
        var existing = updateRepository
                .findByNegotiation_IdAndClientUpdateId(
                        negotiation.getId(),
                        INITIAL_UPDATE_ID
                );

        if (existing.isPresent()) {
            return mapToResponse(existing.get());
        }

        byte[] payload;

        try {
            payload = Base64.getDecoder().decode(
                    request.getUpdatePayload()
            );
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(
                    "Invalid initial collaboration update encoding"
            );
        }

        if (payload.length == 0 || payload.length > MAX_UPDATE_BYTES) {
            throw new BusinessException(
                    "Initial document update must contain between 1 byte and 1 MB"
            );
        }

        User sender = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        AgreementCollaborationUpdate initialUpdate =
                AgreementCollaborationUpdate.builder()
                        .negotiation(negotiation)
                        .sender(sender)
                        .clientUpdateId(INITIAL_UPDATE_ID)
                        .updatePayload(payload)
                        .build();

        AgreementCollaborationUpdate saved =
                updateRepository.saveAndFlush(initialUpdate);

        return mapToResponse(saved);
    }


    private Negotiation getAuthorizedNegotiation(
            UUID negotiationId,
            UUID userId) {

        Negotiation negotiation = negotiationRepository.findById(negotiationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Negotiation not found")
                );

        assertNegotiationParticipant(negotiation, userId);

        return negotiation;
    }


    private Negotiation getAuthorizedNegotiationForUpdate(
            UUID negotiationId,
            UUID userId) {

        Negotiation negotiation =
                negotiationRepository.findByIdForUpdate(negotiationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Negotiation not found"
                                )
                        );

        assertNegotiationParticipant(negotiation, userId);

        return negotiation;
    }


    private void assertNegotiationParticipant(
            Negotiation negotiation,
            UUID userId) {

        if (userId == null
                || (!negotiation.getRequester().getId().equals(userId)
                && !negotiation.getProvider().getId().equals(userId))) {

            throw new BusinessException(
                    "You are not a participant in this negotiation"
            );
        }
    }



    private AgreementVersion getAuthorizedDraftForUpdate(
            UUID negotiationId,
            UUID versionId,
            UUID userId) {

        Negotiation negotiation = negotiationRepository.findById(negotiationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Negotiation not found"));

        if (userId == null
                || (!negotiation.getRequester().getId().equals(userId)
                && !negotiation.getProvider().getId().equals(userId))) {

            throw new BusinessException(
                    "You are not a participant in this negotiation");
        }

        AgreementVersion version =
                versionRepository.findByIdForUpdate(versionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agreement version not found"));

        if (!version.getNegotiation().getId().equals(negotiationId)) {
            throw new BusinessException(
                    "Agreement version does not belong to this negotiation");
        }

        if (version.getStatus() != AgreementVersionStatus.DRAFT) {
            throw new BusinessException(
                    "Collaborative editing is allowed only for draft versions");
        }

        return version;
    }



    private AgreementVersion getAuthorizedDraft(
            UUID negotiationId,
            UUID versionId,
            UUID userId) {

        Negotiation negotiation = negotiationRepository.findById(negotiationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Negotiation not found"));

        if (userId == null
                || (!negotiation.getRequester().getId().equals(userId)
                && !negotiation.getProvider().getId().equals(userId))) {

            throw new BusinessException(
                    "You are not a participant in this negotiation");
        }

        AgreementVersion version = versionRepository.findById(versionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Agreement version not found"));

        if (!version.getNegotiation().getId().equals(negotiationId)) {
            throw new BusinessException(
                    "Agreement version does not belong to this negotiation");
        }

        if (version.getStatus() != AgreementVersionStatus.DRAFT) {
            throw new BusinessException(
                    "Collaborative editing is allowed only for draft versions");
        }

        return version;
    }

    private boolean isValidUuid(String value) {

        if (value == null || value.isBlank()) {
            return false;
        }

        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private AgreementCollaborationUpdateResponseDto mapToResponse(
            AgreementCollaborationUpdate update) {

        AgreementCollaborationUpdateResponseDto dto =
                new AgreementCollaborationUpdateResponseDto();

        dto.setClientUpdateId(update.getClientUpdateId());
        dto.setSenderId(update.getSender().getId());

        dto.setUpdatePayload(
                Base64.getEncoder().encodeToString(
                        update.getUpdatePayload())
        );

        dto.setCreatedAt(update.getCreatedAt());

        return dto;
    }
}
