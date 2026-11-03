
package com.barterlink.agreements.service.impl;

import com.barterlink.agreements.entity.AgreementMilestone;
import com.barterlink.agreements.entity.AgreementVersion;
import com.barterlink.agreements.entity.AgreementVersionMilestone;
import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.enums.AgreementMilestoneStatus;
import com.barterlink.agreements.enums.AgreementReleaseMode;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.agreements.enums.AgreementVersionStatus;
import com.barterlink.agreements.repository.AgreementMilestoneRepository;
import com.barterlink.agreements.repository.AgreementRepository;
import com.barterlink.agreements.repository.AgreementVersionMilestoneRepository;
import com.barterlink.auth.entities.User;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.negotiation.entity.Negotiation;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.enums.SessionStatus;
import com.barterlink.sessions.repository.ServiceSessionRepository;
import com.barterlink.wallet.entity.Wallet;
import com.barterlink.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.barterlink.wallet.service.WalletService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MilestoneCompletionStateService {

    private final AgreementMilestoneRepository milestoneRepository;
    private final AgreementRepository agreementRepository;
    private final AgreementVersionMilestoneRepository versionMilestoneRepository;
    private final ServiceSessionRepository sessionRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;

    @Transactional
    public ConfirmationDecision recordCompletion(
            UUID milestoneId,
            UUID userId) {

        if (milestoneId == null || userId == null) {
            throw new BusinessException(
                    "Milestone and user are required");
        }

        AgreementMilestone initialMilestone =
                milestoneRepository.findById(milestoneId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Milestone not found"));

        UUID negotiationId =
                initialMilestone.getNegotiation().getId();

        AgreementMilestone milestone =
                milestoneRepository
                        .findByIdAndNegotiationIdForUpdate(
                                milestoneId,
                                negotiationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Milestone not found"));

        Negotiation negotiation = milestone.getNegotiation();

        ServiceAgreement agreement =
                agreementRepository.findByNegotiation(negotiation)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agreement not found"));

        User requester = agreement.getRequester();
        User provider = agreement.getProvider();

        UUID requesterId = requester.getId();
        UUID providerId = provider.getId();

        if (!requesterId.equals(userId)
                && !providerId.equals(userId)) {
            throw new BusinessException(
                    "Only the requester or provider can confirm this milestone");
        }

        if (agreement.getReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE
                || agreement.getExchangeMode()
                == ExchangeMode.DIRECT_BARTER) {
            throw new BusinessException(
                    "This operation is only available for Milestone Release agreements");
        }

        AgreementVersion activeVersion =
                agreement.getActiveVersion();

        if (activeVersion == null
                || activeVersion.getStatus()
                != AgreementVersionStatus.ACTIVE
                || activeVersion.getReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE) {
            throw new BusinessException(
                    "There is no active Milestone Release agreement version");
        }

        List<AgreementVersionMilestone> plan =
                versionMilestoneRepository
                        .findByVersion_IdOrderByMilestoneNumberAsc(
                                activeVersion.getId());

        AgreementVersionMilestone currentItem =
                plan.stream()
                        .filter(item ->
                                item.getMilestone() != null
                                        && item.getMilestone().getId()
                                        .equals(milestoneId))
                        .findFirst()
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Milestone is not part of the active agreement version"));

        if (milestone.getStatus()
                == AgreementMilestoneStatus.RELEASED) {
            return decision(
                    milestone,
                    currentItem,
                    false,
                    "This milestone has already been released");
        }

        if (milestone.getStatus()
                == AgreementMilestoneStatus.RELEASE_PENDING) {

            if (milestone.getRequesterCompletionConfirmedAt() == null
                    || milestone.getProviderCompletionConfirmedAt() == null) {
                throw new BusinessException(
                        "Release is pending without both completion confirmations");
            }

            ServiceSession pendingSession =
                    sessionRepository.findByAgreement(agreement)
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Session not found for agreement"));

            return settlementDecision(
                    milestone,
                    currentItem,
                    agreement,
                    pendingSession,
                    "Both participants confirmed; checking milestone escrow settlement");
        }

        if (milestone.getStatus()
                != AgreementMilestoneStatus.FUNDED) {
            throw new BusinessException(
                    "Only the currently funded milestone can be confirmed");
        }

        if (milestone.getFundedAt() == null
                || milestone.getBlockchainEscrowId() == null
                || milestone.getBlockchainEscrowId().isBlank()
                || currentItem.getTokenAllocation() == null
                || !currentItem.getTokenAllocation()
                .equals(milestone.getEscrowLockedTokens())) {
            throw new BusinessException(
                    "Milestone funding details do not match the active agreement");
        }

        /*
         * Enforce sequential progress. A later milestone cannot be
         * confirmed until every earlier milestone has been released.
         */
        for (AgreementVersionMilestone item : plan) {

            AgreementMilestone earlierOrCurrent =
                    item.getMilestone();

            if (earlierOrCurrent == null) {
                throw new BusinessException(
                        "The active milestone plan contains an invalid milestone");
            }

            if (earlierOrCurrent.getStatus()
                    == AgreementMilestoneStatus.CANCELLED) {
                continue;
            }

            if (earlierOrCurrent.getStatus()
                    != AgreementMilestoneStatus.RELEASED) {

                if (!earlierOrCurrent.getId().equals(milestoneId)) {
                    throw new BusinessException(
                            "Milestones must be completed in their agreed order");
                }

                break;
            }
        }

        ServiceSession session =
                sessionRepository.findByAgreement(agreement)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Session not found for agreement"));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException(
                    "The session must be in progress to confirm a milestone");
        }

        Instant now = Instant.now();

        if (requesterId.equals(userId)) {

            if (milestone.getRequesterCompletionConfirmedAt() == null) {
                milestone.setRequesterCompletionConfirmedAt(now);
            }

        } else {

            if (milestone.getProviderCompletionConfirmedAt() == null) {
                milestone.setProviderCompletionConfirmedAt(now);
            }
        }

        boolean bothConfirmed =
                milestone.getRequesterCompletionConfirmedAt() != null
                        && milestone.getProviderCompletionConfirmedAt() != null;

        boolean releaseTriggered = false;

        if (bothConfirmed
                && milestone.getStatus()
                == AgreementMilestoneStatus.FUNDED) {

            /*
             * This transition is committed before any blockchain
             * operation. Only the transaction that performs this
             * transition should initiate the release operation.
             */
            milestone.setStatus(
                    AgreementMilestoneStatus.RELEASE_PENDING);

            releaseTriggered = true;
        }

        milestoneRepository.save(milestone);

        ConfirmationDecision result =
                decision(
                        milestone,
                        currentItem,
                        releaseTriggered,
                        bothConfirmed
                                ? "Both participants confirmed; milestone is ready for escrow settlement"
                                : "Confirmation recorded; waiting for the other participant");

        if (releaseTriggered) {
            result = settlementDecision(
                    milestone,
                    currentItem,
                    agreement,
                    session,
                    "Both participants confirmed; milestone is ready for escrow settlement");
        }

        return result;
    }


    @Transactional
    public boolean claimFundingAttempt(UUID milestoneId) {

        AgreementMilestone initialMilestone =
                milestoneRepository.findById(milestoneId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Milestone not found"));

        AgreementMilestone milestone =
                milestoneRepository
                        .findByIdAndNegotiationIdForUpdate(
                                milestoneId,
                                initialMilestone.getNegotiation().getId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Milestone not found"));

        if (milestone.getStatus()
                != AgreementMilestoneStatus.FUNDING_PENDING) {
            throw new BusinessException(
                    "Milestone is not awaiting funding");
        }

        if (milestone.getBlockchainEscrowId() == null
                || milestone.getBlockchainEscrowId().isBlank()) {
            throw new BusinessException(
                    "Milestone escrow ID is missing");
        }

        if (milestone.getFundingAttemptStartedAt() != null) {
            return false;
        }

        milestone.setFundingAttemptStartedAt(Instant.now());
        milestoneRepository.save(milestone);

        return true;
    }


    @Transactional
    public boolean claimReleaseAttempt(UUID milestoneId) {

        AgreementMilestone initialMilestone =
                milestoneRepository.findById(milestoneId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Milestone not found"));

        AgreementMilestone milestone =
                milestoneRepository
                        .findByIdAndNegotiationIdForUpdate(
                                milestoneId,
                                initialMilestone.getNegotiation().getId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Milestone not found"));

        if (milestone.getStatus()
                != AgreementMilestoneStatus.RELEASE_PENDING) {
            throw new BusinessException(
                    "Milestone is not awaiting escrow release");
        }

        if (milestone.getRequesterCompletionConfirmedAt() == null
                || milestone.getProviderCompletionConfirmedAt() == null) {
            throw new BusinessException(
                    "Both participants must confirm completion before release");
        }

        Negotiation negotiation = milestone.getNegotiation();

        ServiceAgreement agreement =
                agreementRepository.findByNegotiation(negotiation)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agreement not found"));

        if (agreement.getReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE
                || agreement.getExchangeMode()
                == ExchangeMode.DIRECT_BARTER) {

            throw new BusinessException(
                    "Agreement is not eligible for milestone release");
        }


        if (agreement.getStatus() != AgreementStatus.ACTIVE) {
            throw new BusinessException(
                    "Only an active agreement can start a new milestone release; "
                            + "reconciliation is required");
        }


        ServiceSession session =
                sessionRepository.findByAgreement(agreement)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Session not found for agreement"));

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException(
                    "The session must be in progress to release a milestone");
        }

        if (milestone.getReleaseAttemptStartedAt() != null) {
            return false;
        }

        milestone.setReleaseAttemptStartedAt(Instant.now());
        milestoneRepository.save(milestone);

        return true;
    }


    @Transactional
    public void markMilestoneReleasedAndTransfer(
            ConfirmationDecision decision) {

        if (decision == null || decision.milestoneId() == null) {
            throw new BusinessException(
                    "Milestone settlement details are required");
        }

        AgreementMilestone initialMilestone =
                milestoneRepository.findById(decision.milestoneId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Milestone not found"));

        UUID negotiationId =
                initialMilestone.getNegotiation().getId();

        AgreementMilestone milestone =
                milestoneRepository
                        .findByIdAndNegotiationIdForUpdate(
                                decision.milestoneId(),
                                negotiationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Milestone not found"));

        Negotiation negotiation = milestone.getNegotiation();

        ServiceAgreement agreement =
                agreementRepository.findByNegotiation(negotiation)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agreement not found"));

        /*
         * Idempotency: a previously committed settlement must not
         * transfer the same milestone allocation again.
         */
        if (milestone.getStatus()
                == AgreementMilestoneStatus.RELEASED) {
            return;
        }

        if (milestone.getStatus()
                != AgreementMilestoneStatus.RELEASE_PENDING) {
            throw new BusinessException(
                    "Milestone is not awaiting settlement");
        }

        if (milestone.getRequesterCompletionConfirmedAt() == null
                || milestone.getProviderCompletionConfirmedAt() == null
                || milestone.getReleaseAttemptStartedAt() == null) {
            throw new BusinessException(
                    "Both completion confirmations and a release attempt are required");
        }

        if (agreement.getActiveVersion() == null
                || agreement.getReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE
                || agreement.getExchangeMode()
                == ExchangeMode.DIRECT_BARTER) {
            throw new BusinessException(
                    "Agreement is not in an active Milestone Release state");
        }

        if (!agreement.getRequester().getId()
                .equals(decision.requesterId())
                || !agreement.getProvider().getId()
                .equals(decision.providerId())) {
            throw new BusinessException(
                    "Settlement participants do not match the agreement");
        }

        if (!milestone.getBlockchainEscrowId()
                .equals(decision.escrowId())) {
            throw new BusinessException(
                    "Settlement escrow does not match the milestone");
        }

        List<AgreementVersionMilestone> activePlan =
                versionMilestoneRepository
                        .findByVersion_IdOrderByMilestoneNumberAsc(
                                agreement.getActiveVersion().getId());

        AgreementVersionMilestone activeItem =
                activePlan.stream()
                        .filter(item ->
                                item.getMilestone() != null
                                        && item.getMilestone().getId()
                                        .equals(milestone.getId()))
                        .findFirst()
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Milestone is missing from the active agreement version"));

        Integer allocation = activeItem.getTokenAllocation();

        if (allocation == null
                || allocation <= 0
                || !allocation.equals(decision.tokenAllocation())
                || !allocation.equals(milestone.getEscrowLockedTokens())) {
            throw new BusinessException(
                    "Milestone allocation does not match its funded amount");
        }

        /*
         * The caller must invoke this method only after independently
         * verifying that the escrow is RELEASED on-chain.
         *
         * Wallet transfer, transaction history, milestone status and
         * agreement escrow mirror are committed in one SQL transaction.
         */
        walletService.transferLockedTokens(
                agreement.getRequester().getId(),
                agreement.getProvider().getId(),
                allocation,
                "Milestone " + activeItem.getMilestoneNumber()
                        + " released; agreement " + agreement.getId());

        milestone.setStatus(AgreementMilestoneStatus.RELEASED);
        milestone.setReleasedAt(Instant.now());
        milestone.setEscrowLockedTokens(0);

        agreement.setEscrowLockedTokens(0);

        milestoneRepository.save(milestone);
        agreementRepository.save(agreement);
    }


    private ConfirmationDecision settlementDecision(
            AgreementMilestone milestone,
            AgreementVersionMilestone item,
            ServiceAgreement agreement,
            ServiceSession session,
            String message) {

        return new ConfirmationDecision(
                milestone.getId(),
                item.getMilestoneNumber(),
                milestone.getStatus(),
                milestone.getRequesterCompletionConfirmedAt() != null,
                milestone.getProviderCompletionConfirmedAt() != null,
                true,
                toBytes32Uuid(agreement.getId()),
                toBytes32Uuid(session.getId()),
                milestone.getBlockchainEscrowId(),
                item.getTokenAllocation(),
                agreement.getRequester().getId(),
                agreement.getProvider().getId(),
                walletAddress(agreement.getRequester().getId()),
                walletAddress(agreement.getProvider().getId()),
                agreement.getId(),
                session.getId(),
                message);
    }

    private String toBytes32Uuid(UUID id) {
        return "0x"
                + "0".repeat(32)
                + id.toString().replace("-", "");
    }

    private String walletAddress(UUID userId) {

        Wallet wallet =
                walletRepository.findByUserId(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Wallet not found"));

        String address = wallet.getBlockchainAddress();

        if (address == null || address.isBlank()) {
            throw new BusinessException(
                    "A participant's blockchain wallet is not provisioned");
        }

        return address;
    }

    private ConfirmationDecision decision(
            AgreementMilestone milestone,
            AgreementVersionMilestone item,
            boolean releaseTriggered,
            String message) {

        return new ConfirmationDecision(
                milestone.getId(),
                item.getMilestoneNumber(),
                milestone.getStatus(),
                milestone.getRequesterCompletionConfirmedAt() != null,
                milestone.getProviderCompletionConfirmedAt() != null,
                releaseTriggered,
                null,
                null,
                milestone.getBlockchainEscrowId(),
                item.getTokenAllocation(),
                null,
                null,
                null,
                null,
                null,
                null,
                message);
    }

    public record ConfirmationDecision(
            UUID milestoneId,
            Integer milestoneNumber,
            AgreementMilestoneStatus status,
            boolean requesterConfirmed,
            boolean providerConfirmed,
            boolean releaseTriggered,
            String agreementIdBytes32,
            String sessionIdBytes32,
            String escrowId,
            Integer tokenAllocation,
            UUID requesterId,
            UUID providerId,
            String requesterAddress,
            String providerAddress,
            UUID agreementId,
            UUID sessionId,
            String message) {
    }
}
