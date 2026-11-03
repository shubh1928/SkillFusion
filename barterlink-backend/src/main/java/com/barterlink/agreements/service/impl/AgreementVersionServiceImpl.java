package com.barterlink.agreements.service.impl;

import com.barterlink.agreements.dto.request.AgreementVersionMilestoneRequestDto;
import com.barterlink.agreements.dto.request.CreateAgreementVersionRequestDto;
import com.barterlink.agreements.dto.response.AgreementVersionMilestoneResponseDto;
import com.barterlink.agreements.dto.response.AgreementVersionResponseDto;
import com.barterlink.agreements.entity.AgreementMilestone;
import com.barterlink.agreements.entity.AgreementVersion;
import com.barterlink.agreements.entity.AgreementVersionMilestone;
import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.enums.AgreementMilestoneStatus;
import com.barterlink.agreements.enums.AgreementReleaseMode;
import com.barterlink.agreements.enums.AgreementVersionStatus;
import com.barterlink.agreements.repository.AgreementMilestoneRepository;
import com.barterlink.agreements.repository.AgreementRepository;
import com.barterlink.agreements.repository.AgreementVersionMilestoneRepository;
import com.barterlink.agreements.repository.AgreementVersionRepository;
import com.barterlink.agreements.service.AgreementVersionService;
import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.negotiation.entity.Negotiation;
import com.barterlink.negotiation.enums.NegotiationStatus;
import com.barterlink.negotiation.repository.NegotiationRepository;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.enums.SessionStatus;
import com.barterlink.sessions.repository.ServiceSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.barterlink.agreements.dto.request.SubmitAgreementVersionRequestDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgreementVersionServiceImpl
        implements AgreementVersionService {

    private final NegotiationRepository negotiationRepository;
    private final UserRepository userRepository;
    private final AgreementRepository agreementRepository;
    private final AgreementMilestoneRepository milestoneRepository;
    private final AgreementVersionRepository versionRepository;
    private final AgreementVersionMilestoneRepository versionMilestoneRepository;
    private final ServiceSessionRepository sessionRepository;

    @Override
    @Transactional
    public AgreementVersionResponseDto createDraft(
            UUID negotiationId,
            UUID userId,
            CreateAgreementVersionRequestDto request) {

        Negotiation negotiation = getNegotiation(negotiationId);
        assertParticipant(negotiation, userId);
        assertChangesAllowed(negotiation);

        if (request == null) {
            throw new BusinessException("Version request is required");
        }

        boolean directBarter =
                request.getExchangeMode() == ExchangeMode.DIRECT_BARTER;

        if (request.getExchangeMode()
                != negotiation.getProposedExchangeMode()) {
            throw new BusinessException(
                    "Exchange mode must match the current negotiated exchange mode");
        }

        AgreementReleaseMode requesterMode =
                negotiation.getRequesterReleaseMode();

        AgreementReleaseMode providerMode =
                negotiation.getProviderReleaseMode();

        AgreementReleaseMode agreedReleaseMode;

        if (directBarter) {
            agreedReleaseMode = null;

            if (!Integer.valueOf(0).equals(
                    request.getAgreedTokenAmount())) {
                throw new BusinessException(
                        "Direct Barter agreements must have a token amount of zero");
            }

            if (request.getMilestones() != null
                    && !request.getMilestones().isEmpty()) {
                throw new BusinessException(
                        "Direct Barter agreements cannot contain token-funded milestones");
            }
        } else {
            if (requesterMode == null
                    || providerMode == null
                    || requesterMode != providerMode) {
                throw new BusinessException(
                        "Both participants must select the same release mode before creating a version");
            }

            agreedReleaseMode = requesterMode;

            if (request.getAgreedTokenAmount() == null
                    || request.getAgreedTokenAmount() <= 0) {
                throw new BusinessException(
                        "Token-based agreements require a positive whole-BLT amount");
            }
        }

        if (request.getAgreedDeadline() == null
                || !request.getAgreedDeadline().isAfter(Instant.now())) {
            throw new BusinessException(
                    "The agreement deadline must be in the future");
        }

        if (!directBarter
                && (request.getAgreedTokenAmount() == null
                || request.getAgreedTokenAmount() <= 0)) {
            throw new BusinessException(
                    "Token-based agreements require a positive whole-BLT amount");
        }

        if (request.getEstimatedHours() == null
                || request.getEstimatedHours() <= 0) {
            throw new BusinessException(
                    "Estimated hours must be greater than zero");
        }

        if (request.getRequestTitle() == null
                || request.getRequestTitle().isBlank()
                || request.getRequestDescription() == null
                || request.getRequestDescription().isBlank()) {
            throw new BusinessException(
                    "Request title and description are required");
        }

        List<AgreementVersion> versions =
                versionRepository
                        .findByNegotiation_IdOrderByVersionNumberDesc(
                                negotiationId);

        boolean unresolvedVersionExists = versions.stream().anyMatch(v ->
                v.getStatus() == AgreementVersionStatus.DRAFT
                        || v.getStatus()
                        == AgreementVersionStatus.PENDING_ACCEPTANCE
                        || v.getStatus()
                        == AgreementVersionStatus.READY_FOR_ACTIVATION);

        if (unresolvedVersionExists) {
            throw new BusinessException(
                    "Resolve the existing draft or pending version before creating another version");
        }

        ServiceAgreement existingAgreement =
                agreementRepository.findByNegotiation(negotiation)
                        .orElse(null);

        if (existingAgreement != null) {
            if (existingAgreement.getActiveVersion() == null) {
                throw new BusinessException(
                        "This existing agreement has no version history and cannot be amended through the new version workflow");
            }

            if (existingAgreement.getReleaseMode() != agreedReleaseMode) {
                throw new BusinessException(
                        "The agreed release mode cannot be changed by an amendment");
            }

            if (existingAgreement.getExchangeMode()
                    != request.getExchangeMode()) {
                throw new BusinessException(
                        "Exchange mode cannot be changed by this amendment");
            }

            if (existingAgreement.getStatus()
                    == com.barterlink.agreements.enums.AgreementStatus.COMPLETED
                    || existingAgreement.getStatus()
                    == com.barterlink.agreements.enums.AgreementStatus.CANCELLED
                    || existingAgreement.getStatus()
                    == com.barterlink.agreements.enums.AgreementStatus.DISPUTED
                    || existingAgreement.getStatus()
                    == com.barterlink.agreements.enums.AgreementStatus.REFUNDED) {
                throw new BusinessException(
                        "This agreement cannot be amended in its current state");
            }
        }

        List<AgreementVersionMilestoneRequestDto> requestedMilestones =
                request.getMilestones() == null
                        ? List.of()
                        : request.getMilestones();

        validatePlan(
                negotiation,
                agreedReleaseMode,
                request.getAgreedTokenAmount(),
                request.getAgreedDeadline(),
                requestedMilestones,
                existingAgreement);


        if (existingAgreement != null
                && !hasSameFundingCommitmentWithRequest(
                existingAgreement.getActiveVersion(),
                request.getExchangeMode(),
                agreedReleaseMode,
                request.getAgreedTokenAmount(),
                requestedMilestones)) {

            throw new BusinessException(
                    "This amendment changes the token amount or milestone "
                            + "funding plan. A funding-adjustment workflow "
                            + "is required before this change can be proposed.");
        }


        User creator = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        int nextVersionNumber = versions.isEmpty()
                ? 1
                : versions.get(0).getVersionNumber() + 1;

        AgreementVersion version = AgreementVersion.builder()
                .negotiation(negotiation)
                .versionNumber(nextVersionNumber)
                .status(AgreementVersionStatus.DRAFT)
                .exchangeMode(request.getExchangeMode())
                .releaseMode(agreedReleaseMode)
                .offerTitle(request.getOfferTitle())
                .offerDescription(request.getOfferDescription())
                .requestTitle(request.getRequestTitle())
                .requestDescription(request.getRequestDescription())
                .agreedTerms(request.getAgreedTerms())
                .estimatedHours(request.getEstimatedHours())
                .agreedTokenAmount(request.getAgreedTokenAmount())
                .agreedDeadline(request.getAgreedDeadline())
                .createdBy(creator)
                .changeSummary(request.getChangeSummary().trim())
                .createdAt(Instant.now())
                .build();

        AgreementVersion savedVersion =
                versionRepository.save(version);

        for (AgreementVersionMilestoneRequestDto item
                : requestedMilestones) {

            AgreementMilestone milestone;

            if (item.getMilestoneId() == null) {
                milestone = milestoneRepository.save(
                        AgreementMilestone.builder()
                                .negotiation(negotiation)
                                .status(AgreementMilestoneStatus.PLANNED)
                                .escrowLockedTokens(0)
                                .build());
            } else {
                milestone = milestoneRepository
                        .findByIdAndNegotiation_Id(
                                item.getMilestoneId(),
                                negotiationId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Milestone does not belong to this negotiation"));
            }

            AgreementVersionMilestone snapshot =
                    AgreementVersionMilestone.builder()
                            .version(savedVersion)
                            .milestone(milestone)
                            .milestoneNumber(item.getMilestoneNumber())
                            .title(item.getTitle().trim())
                            .deliverable(item.getDeliverable().trim())
                            .deadline(item.getDeadline())
                            .tokenAllocation(item.getTokenAllocation())
                            .build();

            versionMilestoneRepository.save(snapshot);
        }

        return mapToResponse(savedVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgreementVersionResponseDto> getVersions(
            UUID negotiationId,
            UUID userId) {

        Negotiation negotiation = getNegotiation(negotiationId);
        assertParticipant(negotiation, userId);

        return versionRepository
                .findByNegotiation_IdOrderByVersionNumberDesc(negotiationId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Override
    @Transactional
    public AgreementVersionResponseDto submitVersion(
            UUID negotiationId,
            UUID versionId,
            UUID userId,
            SubmitAgreementVersionRequestDto request) {

        Negotiation negotiation = getNegotiation(negotiationId);

        assertParticipant(negotiation, userId);
        assertChangesAllowed(negotiation);

        if (request == null) {
            throw new BusinessException(
                    "Agreement submission data is required");
        }

        /*
         * Lock the version row so a collaborative update
         * cannot race with the final submission.
         */
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
                    "Only a draft version can be submitted");
        }

        if (request.getAgreedDeadline() == null
                || !request.getAgreedDeadline().isAfter(Instant.now())) {

            throw new BusinessException(
                    "The agreement deadline must be in the future");
        }

        if (request.getChangeSummary() == null
                || request.getChangeSummary().isBlank()) {

            throw new BusinessException(
                    "Agreement summary is required");
        }

        List<AgreementVersionMilestoneRequestDto> submittedMilestones =
                request.getMilestones() == null
                        ? List.of()
                        : request.getMilestones();

        ServiceAgreement existingAgreement =
                agreementRepository.findByNegotiation(negotiation)
                        .orElse(null);

        /*
         * Revalidate the complete milestone and funding plan
         * using the final values supplied by the shared editor.
         */
        validatePlan(
                negotiation,
                version.getReleaseMode(),
                version.getAgreedTokenAmount(),
                request.getAgreedDeadline(),
                submittedMilestones,
                existingAgreement);

        List<AgreementVersionMilestone> savedMilestones =
                versionMilestoneRepository
                        .findByVersion_IdOrderByMilestoneNumberAsc(versionId);

        if (savedMilestones.size() != submittedMilestones.size()) {
            throw new BusinessException(
                    "Submitted milestone count does not match the draft");
        }

        Map<Integer, AgreementVersionMilestoneRequestDto> submittedByNumber =
                new HashMap<>();

        for (AgreementVersionMilestoneRequestDto item : submittedMilestones) {
            submittedByNumber.put(item.getMilestoneNumber(), item);
        }

        /*
         * Update the version-specific snapshots.
         * Stable AgreementMilestone identities are not replaced.
         */
        for (AgreementVersionMilestone savedMilestone : savedMilestones) {

            AgreementVersionMilestoneRequestDto submitted =
                    submittedByNumber.get(savedMilestone.getMilestoneNumber());

            if (submitted == null
                    || submitted.getMilestoneId() == null
                    || !savedMilestone.getMilestone().getId().equals(
                    submitted.getMilestoneId())) {

                throw new BusinessException(
                        "Submitted milestone identity does not match the existing draft");
            }

            savedMilestone.setTitle(submitted.getTitle().trim());

            savedMilestone.setDeliverable(
                    submitted.getDeliverable().trim());

            savedMilestone.setDeadline(submitted.getDeadline());

            savedMilestone.setTokenAllocation(
                    submitted.getTokenAllocation());
        }

        versionMilestoneRepository.saveAll(savedMilestones);

        User submitter = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        /*
         * Persist the final shared agreement content.
         */
        version.setAgreedTerms(
                request.getAgreedTerms() == null
                        || request.getAgreedTerms().isBlank()
                        ? null
                        : request.getAgreedTerms().trim());

        version.setAgreedDeadline(request.getAgreedDeadline());

        version.setChangeSummary(request.getChangeSummary().trim());

        version.setSubmittedBy(submitter);

        Instant submittedAt = Instant.now();

        version.setSubmittedAt(submittedAt);

        /*
         * Submission records the submitter's confirmation.
         * The other participant must explicitly accept.
         */
        if (negotiation.getRequester().getId().equals(userId)) {

            version.setRequesterAcceptedAt(submittedAt);

        } else {

            version.setProviderAcceptedAt(submittedAt);
        }

        version.setStatus(
                AgreementVersionStatus.PENDING_ACCEPTANCE);

        return mapToResponse(
                versionRepository.save(version));
    }


    @Override
    @Transactional
    public AgreementVersionResponseDto acceptVersion(
            UUID negotiationId,
            UUID versionId,
            UUID userId) {

        Negotiation negotiation = getNegotiation(negotiationId);
        AgreementVersion version =
                getVersion(negotiationId, versionId);

        assertParticipant(negotiation, userId);
        assertChangesAllowed(negotiation);

        if (version.getSubmittedBy() != null
                && version.getSubmittedBy().getId().equals(userId)) {

            throw new BusinessException(
                    "You submitted this agreement. Only the other participant can accept it.");
        }

        if (negotiation.getRequester().getId().equals(userId)) {
            if (version.getRequesterAcceptedAt() != null) {
                throw new BusinessException(
                        "Requester has already accepted this version");
            }

            version.setRequesterAcceptedAt(Instant.now());

        } else {
            if (version.getProviderAcceptedAt() != null) {
                throw new BusinessException(
                        "Provider has already accepted this version");
            }

            version.setProviderAcceptedAt(Instant.now());
        }

        if (version.getRequesterAcceptedAt() != null
                && version.getProviderAcceptedAt() != null) {

            version.setStatus(
                    AgreementVersionStatus.READY_FOR_ACTIVATION);

            ServiceAgreement existingAgreement =
                    agreementRepository.findByNegotiation(negotiation)
                            .orElse(null);

            if (existingAgreement != null
                    && existingAgreement.getActiveVersion() != null) {

                activateAmendmentIfFundingUnchanged(
                        version,
                        existingAgreement);
            }
        }

        return mapToResponse(versionRepository.save(version));
    }

    @Override
    @Transactional
    public AgreementVersionResponseDto rejectVersion(
            UUID negotiationId,
            UUID versionId,
            UUID userId) {

        Negotiation negotiation = getNegotiation(negotiationId);
        AgreementVersion version =
                getVersion(negotiationId, versionId);

        assertParticipant(negotiation, userId);
        assertChangesAllowed(negotiation);

        if (version.getStatus() != AgreementVersionStatus.DRAFT
                && version.getStatus()
                != AgreementVersionStatus.PENDING_ACCEPTANCE) {
            throw new BusinessException(
                    "Only a draft or pending version can be rejected");
        }

        version.setStatus(AgreementVersionStatus.REJECTED);

        return mapToResponse(versionRepository.save(version));
    }

    @Override
    @Transactional(readOnly = true)
    public AgreementVersion requireReadyInitialVersion(
            UUID negotiationId) {

        List<AgreementVersion> readyVersions =
                versionRepository
                        .findByNegotiation_IdAndStatusOrderByVersionNumberDesc(
                                negotiationId,
                                AgreementVersionStatus.READY_FOR_ACTIVATION);

        if (readyVersions.isEmpty()) {
            throw new BusinessException(
                    "Both participants must accept an agreement version before session acceptance");
        }

        return readyVersions.get(0);
    }

    @Override
    @Transactional
    public void activateInitialVersion(
            AgreementVersion version,
            ServiceAgreement agreement) {

        if (version.getVersionNumber() == null
                || version.getStatus()
                != AgreementVersionStatus.READY_FOR_ACTIVATION
                || version.getRequesterAcceptedAt() == null
                || version.getProviderAcceptedAt() == null) {
            throw new BusinessException(
                    "Agreement version is not ready for activation");
        }

        if (agreement.getReleaseMode() != version.getReleaseMode()) {
            throw new BusinessException(
                    "Agreement release mode does not match the accepted version");
        }

        if (version.getExchangeMode() == ExchangeMode.DIRECT_BARTER) {
            List<AgreementVersionMilestone> milestones =
                    versionMilestoneRepository
                            .findByVersion_IdOrderByMilestoneNumberAsc(
                                    version.getId());

            if (agreement.getExchangeMode() != ExchangeMode.DIRECT_BARTER
                    || !Integer.valueOf(0).equals(
                    version.getAgreedTokenAmount())
                    || !Integer.valueOf(0).equals(
                    agreement.getAgreedTokenAmount())
                    || !milestones.isEmpty()) {
                throw new BusinessException(
                        "Direct Barter activation requires zero token amount and no funded milestones");
            }

        } else if (version.getReleaseMode()
                == AgreementReleaseMode.MILESTONE_RELEASE) {

            List<AgreementVersionMilestone> milestones =
                    versionMilestoneRepository
                            .findByVersion_IdOrderByMilestoneNumberAsc(
                                    version.getId());

            if (milestones.size() < 2 || milestones.size() > 5) {
                throw new BusinessException(
                        "Milestone Release requires between 2 and 5 milestones");
            }

            AgreementVersionMilestone firstItem = milestones.get(0);

            AgreementMilestone firstMilestone = firstItem.getMilestone();

            if (!Integer.valueOf(1).equals(firstItem.getMilestoneNumber())
                    || firstMilestone == null
                    || firstItem.getTokenAllocation() == null
                    || firstMilestone.getStatus()
                    != AgreementMilestoneStatus.FUNDED
                    || firstMilestone.getFundedAt() == null
                    || firstMilestone.getBlockchainEscrowId() == null
                    || firstMilestone.getBlockchainEscrowId().isBlank()
                    || firstMilestone.getEscrowLockedTokens() == null
                    || !firstItem.getTokenAllocation().equals(
                    firstMilestone.getEscrowLockedTokens())) {

                throw new BusinessException(
                        "Milestone 1 must be funded before the version can be activated");
            }

        } else if (version.getReleaseMode()
                != AgreementReleaseMode.DIRECT_RELEASE) {

            throw new BusinessException(
                    "Unsupported agreement release mode");
        }

        version.setStatus(AgreementVersionStatus.ACTIVE);
        version.setActivatedAt(Instant.now());

        agreement.setActiveVersion(version);

        versionRepository.save(version);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasAnyVersion(UUID negotiationId) {
        return !versionRepository
                .findByNegotiation_IdOrderByVersionNumberDesc(
                        negotiationId)
                .isEmpty();
    }


    private void activateAmendmentIfFundingUnchanged(
            AgreementVersion proposedVersion,
            ServiceAgreement agreement) {

        AgreementVersion activeVersion =
                agreement.getActiveVersion();

        if (activeVersion == null
                || activeVersion.getStatus()
                != AgreementVersionStatus.ACTIVE) {

            throw new BusinessException(
                    "The current agreement version is not in a valid active state");
        }

        if (activeVersion.getId().equals(proposedVersion.getId())) {
            throw new BusinessException(
                    "The proposed version is already the active version");
        }

        if (!hasSameFundingCommitment(
                activeVersion,
                proposedVersion)) {

            proposedVersion.setActivationFailureReason(
                    "Both participants accepted this amendment, but the "
                            + "token amount or milestone funding plan changed. "
                            + "The current active version remains in force "
                            + "until the required funding adjustment is completed.");

            return;
        }

        Instant now = Instant.now();

        activeVersion.setStatus(
                AgreementVersionStatus.SUPERSEDED);
        activeVersion.setSupersededAt(now);

        proposedVersion.setStatus(
                AgreementVersionStatus.ACTIVE);
        proposedVersion.setActivatedAt(now);
        proposedVersion.setActivationFailureReason(null);

        agreement.setActiveVersion(proposedVersion);

        agreement.setFinalOfferTitle(
                proposedVersion.getOfferTitle());
        agreement.setFinalOfferDescription(
                proposedVersion.getOfferDescription());
        agreement.setFinalRequestTitle(
                proposedVersion.getRequestTitle());
        agreement.setFinalRequestDescription(
                proposedVersion.getRequestDescription());
        agreement.setExchangeMode(
                proposedVersion.getExchangeMode());
        agreement.setReleaseMode(
                proposedVersion.getReleaseMode());
        agreement.setEstimatedHours(
                proposedVersion.getEstimatedHours());
        agreement.setAgreedTokenAmount(
                proposedVersion.getAgreedTokenAmount());

        versionRepository.save(activeVersion);
        agreementRepository.save(agreement);
    }

    private boolean hasSameFundingCommitment(
            AgreementVersion activeVersion,
            AgreementVersion proposedVersion) {

        if (!java.util.Objects.equals(
                activeVersion.getExchangeMode(),
                proposedVersion.getExchangeMode())
                || !java.util.Objects.equals(
                activeVersion.getReleaseMode(),
                proposedVersion.getReleaseMode())
                || !java.util.Objects.equals(
                activeVersion.getAgreedTokenAmount(),
                proposedVersion.getAgreedTokenAmount())) {

            return false;
        }

        if (activeVersion.getReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE) {

            return true;
        }

        List<AgreementVersionMilestone> activeMilestones =
                versionMilestoneRepository
                        .findByVersion_IdOrderByMilestoneNumberAsc(
                                activeVersion.getId());

        List<AgreementVersionMilestone> proposedMilestones =
                versionMilestoneRepository
                        .findByVersion_IdOrderByMilestoneNumberAsc(
                                proposedVersion.getId());

        if (activeMilestones.size()
                != proposedMilestones.size()) {

            return false;
        }

        for (int i = 0; i < activeMilestones.size(); i++) {

            AgreementVersionMilestone activeItem =
                    activeMilestones.get(i);

            AgreementVersionMilestone proposedItem =
                    proposedMilestones.get(i);

            if (activeItem.getMilestone() == null
                    || proposedItem.getMilestone() == null
                    || !activeItem.getMilestone().getId().equals(
                    proposedItem.getMilestone().getId())
                    || !java.util.Objects.equals(
                    activeItem.getMilestoneNumber(),
                    proposedItem.getMilestoneNumber())
                    || !java.util.Objects.equals(
                    activeItem.getTokenAllocation(),
                    proposedItem.getTokenAllocation())) {

                return false;
            }
        }

        return true;
    }

    private boolean hasSameFundingCommitmentWithRequest(
            AgreementVersion activeVersion,
            ExchangeMode proposedExchangeMode,
            AgreementReleaseMode proposedReleaseMode,
            Integer proposedTokenAmount,
            List<AgreementVersionMilestoneRequestDto> requestedMilestones) {

        if (activeVersion == null
                || !Objects.equals(
                activeVersion.getExchangeMode(),
                proposedExchangeMode)
                || !Objects.equals(
                activeVersion.getReleaseMode(),
                proposedReleaseMode)
                || !Objects.equals(
                activeVersion.getAgreedTokenAmount(),
                proposedTokenAmount)) {

            return false;
        }

        if (proposedReleaseMode
                != AgreementReleaseMode.MILESTONE_RELEASE) {
            return true;
        }

        List<AgreementVersionMilestone> activeMilestones =
                versionMilestoneRepository
                        .findByVersion_IdOrderByMilestoneNumberAsc(
                                activeVersion.getId());

        if (activeMilestones.size() != requestedMilestones.size()) {
            return false;
        }

        Map<Integer, AgreementVersionMilestoneRequestDto> requestedByNumber =
                new HashMap<>();

        for (AgreementVersionMilestoneRequestDto item
                : requestedMilestones) {
            requestedByNumber.put(
                    item.getMilestoneNumber(),
                    item);
        }

        for (AgreementVersionMilestone activeItem : activeMilestones) {

            AgreementVersionMilestoneRequestDto proposedItem =
                    requestedByNumber.get(
                            activeItem.getMilestoneNumber());

            if (proposedItem == null
                    || activeItem.getMilestone() == null
                    || proposedItem.getMilestoneId() == null
                    || !activeItem.getMilestone().getId().equals(
                    proposedItem.getMilestoneId())
                    || !Objects.equals(
                    activeItem.getTokenAllocation(),
                    proposedItem.getTokenAllocation())) {

                return false;
            }
        }

        return true;
    }


    private void validatePlan(
            Negotiation negotiation,
            AgreementReleaseMode releaseMode,
            Integer agreedTokenAmount,
            Instant agreedDeadline,
            List<AgreementVersionMilestoneRequestDto> items,
            ServiceAgreement existingAgreement) {


        if (negotiation.getProposedExchangeMode()
                == ExchangeMode.DIRECT_BARTER) {

            if (releaseMode != null
                    || !Integer.valueOf(0).equals(agreedTokenAmount)
                    || !items.isEmpty()) {
                throw new BusinessException(
                        "Direct Barter agreements do not use token release modes or funded milestones");
            }

            return;
        }

        if (releaseMode == AgreementReleaseMode.DIRECT_RELEASE) {
            if (!items.isEmpty()) {
                throw new BusinessException(
                        "Direct Release agreements cannot contain a milestone plan");
            }
            return;
        }

        Integer requesterCount =
                negotiation.getRequesterMilestoneCount();

        Integer providerCount =
                negotiation.getProviderMilestoneCount();

        if (requesterCount == null || providerCount == null
                || requesterCount < 2 || requesterCount > 5
                || !requesterCount.equals(providerCount)) {
            throw new BusinessException(
                    "Both participants must select the same milestone count from 2 to 5");
        }

        if (items.size() != requesterCount) {
            throw new BusinessException(
                    "The milestone plan must contain exactly the agreed milestone count");
        }

        Set<Integer> numbers = new HashSet<>();
        Set<UUID> suppliedIds = new HashSet<>();
        long total = 0L;
        Instant now = Instant.now();

        for (AgreementVersionMilestoneRequestDto item : items) {
            if (item == null
                    || item.getMilestoneNumber() == null
                    || item.getMilestoneNumber() < 1
                    || item.getMilestoneNumber() > requesterCount
                    || !numbers.add(item.getMilestoneNumber())) {
                throw new BusinessException(
                        "Milestone numbers must be unique and consecutive from 1");
            }

            if (item.getTitle() == null || item.getTitle().isBlank()
                    || item.getDeliverable() == null
                    || item.getDeliverable().isBlank()) {
                throw new BusinessException(
                        "Every milestone requires a title and deliverable");
            }

            if (item.getDeadline() == null
                    || !item.getDeadline().isAfter(now)
                    || item.getDeadline().isAfter(agreedDeadline)) {
                throw new BusinessException(
                        "Each milestone deadline must be in the future and no later than the agreement deadline");
            }

            if (item.getTokenAllocation() == null
                    || item.getTokenAllocation() <= 0) {
                throw new BusinessException(
                        "Every milestone allocation must be a positive whole-BLT amount");
            }

            total += item.getTokenAllocation();

            if (item.getMilestoneId() != null) {
                if (!suppliedIds.add(item.getMilestoneId())) {
                    throw new BusinessException(
                            "A milestone cannot appear more than once in a version");
                }

                AgreementMilestone milestone =
                        milestoneRepository.findByIdAndNegotiation_Id(
                                        item.getMilestoneId(),
                                        negotiation.getId())
                                .orElseThrow(() ->
                                        new BusinessException(
                                                "Milestone does not belong to this negotiation"));

                if (milestone.getStatus()
                        == AgreementMilestoneStatus.CANCELLED) {
                    throw new BusinessException(
                            "A cancelled milestone cannot be reused");
                }

                if (isExecutionStarted(milestone)) {
                    ensureMilestoneTermsUnchanged(
                            milestone,
                            item,
                            existingAgreement);
                }
            }
        }

        for (int i = 1; i <= requesterCount; i++) {
            if (!numbers.contains(i)) {
                throw new BusinessException(
                        "Milestone numbers must be consecutive from 1");
            }
        }

        if (total != agreedTokenAmount.longValue()) {
            throw new BusinessException(
                    "Milestone allocations must total exactly the agreed BLT amount");
        }

        for (AgreementMilestone milestone :
                milestoneRepository.findByNegotiation_IdOrderByCreatedAtAsc(
                        negotiation.getId())) {

            if (isExecutionStarted(milestone)
                    && !suppliedIds.contains(milestone.getId())) {
                throw new BusinessException(
                        "An active, submitted, disputed, funded, or released milestone cannot be removed from a new version");
            }
        }
    }

    private boolean isExecutionStarted(AgreementMilestone milestone) {
        return (milestone.getStatus() != AgreementMilestoneStatus.PLANNED
                && milestone.getStatus() != AgreementMilestoneStatus.CANCELLED)
                || (milestone.getEscrowLockedTokens() != null
                && milestone.getEscrowLockedTokens() > 0)
                || milestone.getReleasedAt() != null
                || milestone.getRefundedAt() != null;
    }

    private void ensureMilestoneTermsUnchanged(
            AgreementMilestone milestone,
            AgreementVersionMilestoneRequestDto proposed,
            ServiceAgreement agreement) {

        if (agreement == null || agreement.getActiveVersion() == null) {
            throw new BusinessException(
                    "Cannot verify prior terms for a milestone whose execution has started");
        }

        List<AgreementVersionMilestone> currentTerms =
                versionMilestoneRepository
                        .findByVersion_IdOrderByMilestoneNumberAsc(
                                agreement.getActiveVersion().getId());

        AgreementVersionMilestone previous =
                currentTerms.stream()
                        .filter(item -> item.getMilestone().getId()
                                .equals(milestone.getId()))
                        .findFirst()
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Active version is missing the milestone's previous terms"));

        boolean unchanged =
                Objects.equals(previous.getMilestoneNumber(),
                        proposed.getMilestoneNumber())
                        && Objects.equals(previous.getTitle(),
                        proposed.getTitle().trim())
                        && Objects.equals(previous.getDeliverable(),
                        proposed.getDeliverable().trim())
                        && Objects.equals(previous.getDeadline(),
                        proposed.getDeadline())
                        && Objects.equals(previous.getTokenAllocation(),
                        proposed.getTokenAllocation());

        if (!unchanged) {
            throw new BusinessException(
                    "Terms for a milestone whose execution has started cannot be changed by an ordinary amendment");
        }
    }

    private void assertChangesAllowed(Negotiation negotiation) {

        NegotiationStatus status = negotiation.getStatus();

        if (status != NegotiationStatus.ACTIVE
                && status != NegotiationStatus.SESSION_REQUESTED
                && status != NegotiationStatus.ACCEPTED) {
            throw new BusinessException(
                    "Agreement versions cannot be changed in the current negotiation state");
        }

        ServiceAgreement agreement =
                agreementRepository.findByNegotiation(negotiation)
                        .orElse(null);

        if (agreement == null) {
            return;
        }

        if (agreement.getStatus()
                == com.barterlink.agreements.enums.AgreementStatus.COMPLETED
                || agreement.getStatus()
                == com.barterlink.agreements.enums.AgreementStatus.CANCELLED
                || agreement.getStatus()
                == com.barterlink.agreements.enums.AgreementStatus.DISPUTED
                || agreement.getStatus()
                == com.barterlink.agreements.enums.AgreementStatus.REFUNDED) {
            throw new BusinessException(
                    "Agreement versions cannot be changed after completion, cancellation, or dispute");
        }

        ServiceSession session =
                sessionRepository.findByAgreement(agreement).orElse(null);

        if (session != null
                && (session.getStatus() == SessionStatus.COMPLETED
                || session.getStatus() == SessionStatus.CANCELLED
                || session.getStatus() == SessionStatus.DISPUTED)) {
            throw new BusinessException(
                    "Agreement versions cannot be changed after the session ends or enters a dispute");
        }
    }

    private void assertParticipant(
            Negotiation negotiation,
            UUID userId) {

        if (userId == null
                || (!negotiation.getRequester().getId().equals(userId)
                && !negotiation.getProvider().getId().equals(userId))) {
            throw new BusinessException(
                    "You are not a participant in this negotiation");
        }
    }

    private Negotiation getNegotiation(UUID negotiationId) {
        return negotiationRepository.findById(negotiationId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Negotiation not found"));
    }

    private AgreementVersion getVersion(
            UUID negotiationId,
            UUID versionId) {

        AgreementVersion version =
                versionRepository.findById(versionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agreement version not found"));

        if (!version.getNegotiation().getId().equals(negotiationId)) {
            throw new BusinessException(
                    "Agreement version does not belong to this negotiation");
        }

        return version;
    }

    private AgreementVersionResponseDto mapToResponse(
            AgreementVersion version) {

        AgreementVersionResponseDto dto =
                new AgreementVersionResponseDto();

        dto.setVersionId(version.getId());
        dto.setNegotiationId(version.getNegotiation().getId());
        dto.setVersionNumber(version.getVersionNumber());
        dto.setStatus(version.getStatus());
        dto.setExchangeMode(version.getExchangeMode());
        dto.setReleaseMode(version.getReleaseMode());

        dto.setOfferTitle(version.getOfferTitle());
        dto.setOfferDescription(version.getOfferDescription());
        dto.setRequestTitle(version.getRequestTitle());
        dto.setRequestDescription(version.getRequestDescription());

        dto.setAgreedTerms(version.getAgreedTerms());

        dto.setEstimatedHours(version.getEstimatedHours());
        dto.setAgreedTokenAmount(version.getAgreedTokenAmount());
        dto.setAgreedDeadline(version.getAgreedDeadline());

        dto.setCreatedById(version.getCreatedBy().getId());
        dto.setSubmittedById(
                version.getSubmittedBy() != null
                        ? version.getSubmittedBy().getId()
                        : null
        );
        dto.setChangeSummary(version.getChangeSummary());

        dto.setCreatedAt(version.getCreatedAt());
        dto.setSubmittedAt(version.getSubmittedAt());
        dto.setRequesterAcceptedAt(version.getRequesterAcceptedAt());
        dto.setProviderAcceptedAt(version.getProviderAcceptedAt());
        dto.setActivatedAt(version.getActivatedAt());
        dto.setSupersededAt(version.getSupersededAt());
        dto.setActivationFailureReason(
                version.getActivationFailureReason());

        List<AgreementVersionMilestoneResponseDto> milestones =
                versionMilestoneRepository
                        .findByVersion_IdOrderByMilestoneNumberAsc(
                                version.getId())
                        .stream()
                        .map(this::mapMilestoneToResponse)
                        .toList();

        dto.setMilestones(milestones);

        return dto;
    }

    private AgreementVersionMilestoneResponseDto mapMilestoneToResponse(
            AgreementVersionMilestone milestoneVersion) {

        AgreementVersionMilestoneResponseDto dto =
                new AgreementVersionMilestoneResponseDto();

        dto.setVersionMilestoneId(milestoneVersion.getId());
        dto.setMilestoneId(milestoneVersion.getMilestone().getId());
        dto.setMilestoneNumber(milestoneVersion.getMilestoneNumber());
        dto.setTitle(milestoneVersion.getTitle());
        dto.setDeliverable(milestoneVersion.getDeliverable());
        dto.setDeadline(milestoneVersion.getDeadline());
        dto.setTokenAllocation(milestoneVersion.getTokenAllocation());
        dto.setExecutionStatus(
                milestoneVersion.getMilestone().getStatus());

        return dto;
    }
}