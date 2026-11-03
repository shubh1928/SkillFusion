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
import com.barterlink.agreements.repository.AgreementVersionRepository;
import com.barterlink.agreements.service.AgreementVersionService;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.negotiation.entity.Negotiation;
import com.barterlink.negotiation.repository.NegotiationRepository;
import com.barterlink.sessions.dto.response.SessionResponseDto;
import com.barterlink.sessions.service.SessionService;
import com.barterlink.wallet.entity.Wallet;
import com.barterlink.wallet.repository.WalletRepository;
import com.barterlink.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Hash;
import org.web3j.utils.Numeric;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.enums.SessionStatus;
import com.barterlink.sessions.repository.ServiceSessionRepository;
import java.util.Optional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MilestoneFundingPreparationService {

    private final NegotiationRepository negotiationRepository;
    private final AgreementRepository agreementRepository;
    private final AgreementVersionRepository versionRepository;
    private final AgreementMilestoneRepository milestoneRepository;
    private final AgreementVersionMilestoneRepository versionMilestoneRepository;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final SessionService sessionService;
    private final ServiceSessionRepository serviceSessionRepository;
    private final AgreementVersionService agreementVersionService;

    /*
     * Prepares the database state inside the current acceptance
     * transaction. Blockchain calls are deliberately not made here.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public PreparedMilestoneFunding prepareInitialFunding(
            UUID negotiationId,
            UUID versionId) {

        Negotiation negotiation =
                negotiationRepository.findById(negotiationId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Negotiation not found"));

        AgreementVersion version =
                versionRepository.findById(versionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agreement version not found"));

        if (!version.getNegotiation().getId().equals(negotiationId)) {
            throw new BusinessException(
                    "Agreement version does not belong to this negotiation");
        }

        if (version.getStatus()
                != AgreementVersionStatus.READY_FOR_ACTIVATION
                || version.getRequesterAcceptedAt() == null
                || version.getProviderAcceptedAt() == null) {
            throw new BusinessException(
                    "Both participants must accept the initial version first");
        }

        if (version.getReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE
                || negotiation.getRequesterReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE
                || negotiation.getProviderReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE) {
            throw new BusinessException(
                    "Milestone Release must match both participants' selections");
        }

        if (version.getExchangeMode() == ExchangeMode.DIRECT_BARTER) {
            throw new BusinessException(
                    "Milestone Release is not available for Direct Barter");
        }

        if (version.getAgreedTokenAmount() == null
                || version.getAgreedTokenAmount() <= 0) {
            throw new BusinessException(
                    "The agreed token amount must be positive");
        }

        Integer requesterCount =
                negotiation.getRequesterMilestoneCount();

        Integer providerCount =
                negotiation.getProviderMilestoneCount();

        if (requesterCount == null
                || providerCount == null
                || requesterCount < 2
                || requesterCount > 5
                || !requesterCount.equals(providerCount)) {
            throw new BusinessException(
                    "Both participants must select the same milestone count from 2 to 5");
        }

        List<AgreementVersionMilestone> plannedMilestones =
                versionMilestoneRepository
                        .findByVersion_IdOrderByMilestoneNumberAsc(
                                version.getId());

        if (plannedMilestones.size() != requesterCount) {
            throw new BusinessException(
                    "The accepted version does not contain the agreed milestone count");
        }

        Set<UUID> milestoneIds = new HashSet<>();
        long allocationTotal = 0L;

        for (int i = 0; i < plannedMilestones.size(); i++) {

            AgreementVersionMilestone item = plannedMilestones.get(i);

            if (item.getMilestone() == null
                    || !Integer.valueOf(i + 1).equals(
                    item.getMilestoneNumber())) {
                throw new BusinessException(
                        "Milestone numbers must be consecutive, starting at 1");
            }

            AgreementMilestone milestone = item.getMilestone();

            if (!milestone.getNegotiation().getId()
                    .equals(negotiationId)) {
                throw new BusinessException(
                        "A milestone does not belong to this negotiation");
            }

            if (!milestoneIds.add(milestone.getId())) {
                throw new BusinessException(
                        "The version contains a duplicate milestone");
            }

            if (item.getTokenAllocation() == null
                    || item.getTokenAllocation() <= 0) {
                throw new BusinessException(
                        "Every milestone must have a positive whole-BLT allocation");
            }

            allocationTotal += item.getTokenAllocation();
        }

        if (allocationTotal
                != version.getAgreedTokenAmount().longValue()) {
            throw new BusinessException(
                    "Milestone allocations must total exactly the agreed BLT amount");
        }

        AgreementVersionMilestone firstItem = plannedMilestones.get(0);

        AgreementMilestone firstMilestone = firstItem.getMilestone();

        if (!Integer.valueOf(1).equals(firstItem.getMilestoneNumber())
                || firstMilestone.getStatus()
                != AgreementMilestoneStatus.PLANNED
                || (firstMilestone.getEscrowLockedTokens() != null
                && firstMilestone.getEscrowLockedTokens() > 0)
                || (firstMilestone.getBlockchainEscrowId() != null
                && !firstMilestone.getBlockchainEscrowId().isBlank())) {
            throw new BusinessException(
                    "The first milestone is not in a clean, fundable state");
        }

        if (agreementRepository.findByNegotiation(negotiation)
                .isPresent()) {
            throw new BusinessException(
                    "An agreement already exists for this negotiation");
        }

        Wallet requesterWallet =
                walletRepository.findByUserId(
                                negotiation.getRequester().getId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Requester blockchain wallet not found"));

        Wallet providerWallet =
                walletRepository.findByUserId(
                                negotiation.getProvider().getId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Provider blockchain wallet not found"));

        String requesterAddress = requesterWallet.getBlockchainAddress();
        String providerAddress = providerWallet.getBlockchainAddress();

        if (requesterAddress == null || requesterAddress.isBlank()) {
            throw new BusinessException(
                    "Requester blockchain wallet is not provisioned");
        }

        if (providerAddress == null || providerAddress.isBlank()) {
            throw new BusinessException(
                    "Provider blockchain wallet is not provisioned");
        }

        ServiceAgreement agreement =
                ServiceAgreement.builder()
                        .negotiation(negotiation)
                        .requester(negotiation.getRequester())
                        .provider(negotiation.getProvider())
                        .finalOfferTitle(version.getOfferTitle())
                        .finalOfferDescription(version.getOfferDescription())
                        .finalRequestTitle(version.getRequestTitle())
                        .finalRequestDescription(version.getRequestDescription())
                        .exchangeMode(version.getExchangeMode())
                        .releaseMode(AgreementReleaseMode.MILESTONE_RELEASE)
                        .estimatedHours(version.getEstimatedHours())
                        .agreedTokenAmount(version.getAgreedTokenAmount())
                        .escrowLockedTokens(0)
                        .status(AgreementStatus.ACTIVE)
                        .build();

        ServiceAgreement savedAgreement =
                agreementRepository.save(agreement);

        SessionResponseDto sessionResponse =
                sessionService.createSession(savedAgreement);

        if (sessionResponse == null
                || sessionResponse.getSessionId() == null) {
            throw new BusinessException(
                    "Session creation did not return a session ID");
        }

        UUID sessionId = sessionResponse.getSessionId();

        String agreementIdBytes32 =
                toBytes32Uuid(savedAgreement.getId());

        String sessionIdBytes32 =
                toBytes32Uuid(sessionId);

        /*
         * Each milestone needs its own escrow key. Do not reuse
         * Direct Release's agreement/session escrow key.
         */
        String fundingKey =
                "BARTERLINK_MILESTONE:"
                        + savedAgreement.getId()
                        + ":"
                        + sessionId
                        + ":"
                        + firstMilestone.getId();

        String escrowId =
                Numeric.toHexString(
                        Hash.sha3(
                                fundingKey.getBytes(StandardCharsets.UTF_8)));

        firstMilestone.setStatus(
                AgreementMilestoneStatus.FUNDING_PENDING);

        firstMilestone.setBlockchainEscrowId(escrowId);
        firstMilestone.setEscrowLockedTokens(0);

        milestoneRepository.save(firstMilestone);

        /*
         * Reserve only milestone 1's allocation in the SQL wallet
         * mirror. This transaction commits before any blockchain
         * transaction is attempted.
         */
        walletService.lockTokens(
                negotiation.getRequester().getId(),
                firstItem.getTokenAllocation(),
                "Milestone 1 funding reservation; agreement "
                        + savedAgreement.getId()
                        + "; escrow "
                        + escrowId);

        return new PreparedMilestoneFunding(
                negotiationId,
                version.getId(),
                savedAgreement.getId(),
                sessionId,
                firstMilestone.getId(),
                negotiation.getRequester().getId(),
                firstItem.getTokenAllocation(),
                requesterAddress,
                providerAddress,
                agreementIdBytes32,
                sessionIdBytes32,
                escrowId);
    }



    /**
     * Prepares the next milestone only after the preceding milestone
     * has been released. Does not submit a blockchain transaction.
     *
     * Returns Optional.empty() when all milestones are already released.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<PreparedMilestoneFunding> prepareNextMilestoneFunding(
            UUID agreementId) {

        if (agreementId == null) {
            throw new BusinessException("Agreement ID is required");
        }

        ServiceAgreement agreement =
                agreementRepository.findById(agreementId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agreement not found"));

        if (agreement.getReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE
                || agreement.getExchangeMode() == ExchangeMode.DIRECT_BARTER) {
            throw new BusinessException(
                    "Next-stage funding is available only for Milestone Release token agreements");
        }

        AgreementVersion version = agreement.getActiveVersion();

        if (version == null
                || version.getStatus() != AgreementVersionStatus.ACTIVE
                || version.getReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE) {
            throw new BusinessException(
                    "An active Milestone Release version is required");
        }

        Negotiation negotiation = agreement.getNegotiation();

        ServiceSession session =
                serviceSessionRepository.findByAgreement(agreement)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Session not found for agreement"));



        List<AgreementVersionMilestone> plan =
                versionMilestoneRepository
                        .findByVersion_IdOrderByMilestoneNumberAsc(
                                version.getId());

        if (plan.size() < 2 || plan.size() > 5) {
            throw new BusinessException(
                    "The active version must contain between 2 and 5 milestones");
        }

        long allocationTotal = 0L;

        for (int i = 0; i < plan.size(); i++) {
            AgreementVersionMilestone item = plan.get(i);

            if (item.getMilestone() == null
                    || !Integer.valueOf(i + 1).equals(
                    item.getMilestoneNumber())
                    || item.getTokenAllocation() == null
                    || item.getTokenAllocation() <= 0) {
                throw new BusinessException(
                        "The active milestone plan is invalid");
            }

            allocationTotal += item.getTokenAllocation();
        }

        if (version.getAgreedTokenAmount() == null
                || allocationTotal
                != version.getAgreedTokenAmount().longValue()) {
            throw new BusinessException(
                    "Active milestone allocations do not equal the agreed BLT amount");
        }

        AgreementVersionMilestone nextItem = null;

        for (AgreementVersionMilestone item : plan) {
            AgreementMilestone milestone = item.getMilestone();

            if (milestone.getStatus()
                    == AgreementMilestoneStatus.RELEASED) {
                continue;
            }

            if (milestone.getStatus()
                    == AgreementMilestoneStatus.PLANNED
                    || milestone.getStatus()
                    == AgreementMilestoneStatus.FUNDING_PENDING) {
                nextItem = item;
                break;
            }

            throw new BusinessException(
                    "The next milestone is not ready for funding; "
                            + "its current state requires resolution");
        }

        if (nextItem == null) {
            return Optional.empty();
        }

        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new BusinessException(
                    "The session must be in progress before funding another milestone");
        }

        AgreementMilestone initialNextMilestone =
                nextItem.getMilestone();

        AgreementMilestone nextMilestone =
                milestoneRepository
                        .findByIdAndNegotiationIdForUpdate(
                                initialNextMilestone.getId(),
                                negotiation.getId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Next milestone not found"));

        boolean alreadyPrepared =
                nextMilestone.getStatus()
                        == AgreementMilestoneStatus.FUNDING_PENDING;

        if (alreadyPrepared) {

            /*
             * Reuse existing preparation, but do not reserve tokens again.
             * The deterministic escrow ID is checked below.
             */
            if (nextMilestone.getBlockchainEscrowId() == null
                    || nextMilestone.getBlockchainEscrowId().isBlank()
                    || (nextMilestone.getEscrowLockedTokens() != null
                    && nextMilestone.getEscrowLockedTokens() != 0)
                    || nextMilestone.getFundedAt() != null
                    || nextMilestone.getReleasedAt() != null
                    || nextMilestone.getRefundedAt() != null
                    || nextMilestone.getRequesterCompletionConfirmedAt() != null
                    || nextMilestone.getProviderCompletionConfirmedAt() != null
                    || nextMilestone.getReleaseAttemptStartedAt() != null) {

                throw new BusinessException(
                        "Existing pending funding preparation requires reconciliation");
            }

        } else if (nextMilestone.getStatus()
                != AgreementMilestoneStatus.PLANNED
                || (nextMilestone.getEscrowLockedTokens() != null
                && nextMilestone.getEscrowLockedTokens() != 0)
                || (nextMilestone.getBlockchainEscrowId() != null
                && !nextMilestone.getBlockchainEscrowId().isBlank())
                || nextMilestone.getRequesterCompletionConfirmedAt() != null
                || nextMilestone.getProviderCompletionConfirmedAt() != null
                || nextMilestone.getReleaseAttemptStartedAt() != null
                || nextMilestone.getFundingAttemptStartedAt() != null) {

            throw new BusinessException(
                    "The next milestone is not in a clean fundable state");
        }

        Integer allocation = nextItem.getTokenAllocation();

        Wallet requesterWallet =
                walletRepository.findByUserId(
                                negotiation.getRequester().getId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Requester wallet not found"));

        Wallet providerWallet =
                walletRepository.findByUserId(
                                negotiation.getProvider().getId())
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Provider wallet not found"));

        String requesterAddress =
                requesterWallet.getBlockchainAddress();

        String providerAddress =
                providerWallet.getBlockchainAddress();

        if (requesterAddress == null || requesterAddress.isBlank()) {
            throw new BusinessException(
                    "Requester blockchain wallet is not provisioned");
        }

        if (providerAddress == null || providerAddress.isBlank()) {
            throw new BusinessException(
                    "Provider blockchain wallet is not provisioned");
        }

        String agreementIdBytes32 =
                toBytes32Uuid(agreement.getId());

        String sessionIdBytes32 =
                toBytes32Uuid(session.getId());

        String fundingKey =
                "BARTERLINK_MILESTONE:"
                        + agreement.getId()
                        + ":"
                        + session.getId()
                        + ":"
                        + nextMilestone.getId();

        String escrowId =
                Numeric.toHexString(
                        Hash.sha3(
                                fundingKey.getBytes(StandardCharsets.UTF_8)));

        PreparedMilestoneFunding prepared =
                new PreparedMilestoneFunding(
                        negotiation.getId(),
                        version.getId(),
                        agreement.getId(),
                        session.getId(),
                        nextMilestone.getId(),
                        negotiation.getRequester().getId(),
                        allocation,
                        requesterAddress,
                        providerAddress,
                        agreementIdBytes32,
                        sessionIdBytes32,
                        escrowId);

        if (alreadyPrepared) {

            if (!escrowId.equalsIgnoreCase(
                    nextMilestone.getBlockchainEscrowId())) {

                throw new BusinessException(
                        "Existing milestone escrow ID does not match "
                                + "the deterministic funding ID");
            }

            /*
             * Do not call lockTokens again. The previous preparation
             * transaction already reserved this allocation.
             * The coordinator must reconcile its blockchain state.
             */
            return Optional.of(prepared);
        }

        walletService.lockTokens(
                negotiation.getRequester().getId(),
                allocation,
                "Milestone " + nextItem.getMilestoneNumber()
                        + " funding reservation; agreement "
                        + agreement.getId()
                        + "; escrow "
                        + escrowId);

        nextMilestone.setStatus(
                AgreementMilestoneStatus.FUNDING_PENDING);

        nextMilestone.setBlockchainEscrowId(escrowId);
        nextMilestone.setEscrowLockedTokens(0);

        milestoneRepository.save(nextMilestone);

        return Optional.of(prepared);
    }



    /*
     * This method runs only after the blockchain service has returned
     * a verified LOCKED escrow. The database status change and V1
     * activation commit together.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFundedAndActivate(
            PreparedMilestoneFunding prepared) {

        Negotiation negotiation =
                negotiationRepository.findById(prepared.negotiationId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Negotiation not found"));

        AgreementVersion version =
                versionRepository.findById(prepared.versionId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agreement version not found"));

        ServiceAgreement agreement =
                agreementRepository.findById(prepared.agreementId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agreement not found"));

        AgreementMilestone milestone =
                milestoneRepository.findByIdAndNegotiation_Id(
                                prepared.milestoneId(),
                                prepared.negotiationId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Milestone not found"));

        if (!version.getNegotiation().getId()
                .equals(prepared.negotiationId())
                || !agreement.getNegotiation().getId()
                .equals(prepared.negotiationId())) {
            throw new BusinessException(
                    "Funding records do not belong to the expected negotiation");
        }

        if (agreement.getReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE
                || version.getReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE
                || agreement.getActiveVersion() != null) {
            throw new BusinessException(
                    "Agreement is not in the expected initial Milestone Release state");
        }

        if (version.getStatus()
                != AgreementVersionStatus.READY_FOR_ACTIVATION
                || version.getRequesterAcceptedAt() == null
                || version.getProviderAcceptedAt() == null) {
            throw new BusinessException(
                    "The initial version is not ready for activation");
        }

        if (milestone.getStatus()
                != AgreementMilestoneStatus.FUNDING_PENDING
                || !prepared.escrowId().equals(
                milestone.getBlockchainEscrowId())) {
            throw new BusinessException(
                    "The milestone funding state or escrow ID does not match");
        }

        List<AgreementVersionMilestone> versionMilestones =
                versionMilestoneRepository
                        .findByVersion_IdOrderByMilestoneNumberAsc(
                                version.getId());

        if (versionMilestones.isEmpty()) {
            throw new BusinessException(
                    "The initial version has no milestone plan");
        }

        AgreementVersionMilestone firstItem = versionMilestones.get(0);

        if (!Integer.valueOf(1).equals(firstItem.getMilestoneNumber())
                || !firstItem.getMilestone().getId()
                .equals(milestone.getId())
                || !firstItem.getTokenAllocation()
                .equals(prepared.tokenAllocation())) {
            throw new BusinessException(
                    "The funded milestone does not match milestone 1 of the accepted version");
        }

        Instant now = Instant.now();

        milestone.setStatus(AgreementMilestoneStatus.FUNDED);
        milestone.setFundedAt(now);
        milestone.setEscrowLockedTokens(
                prepared.tokenAllocation());

        agreement.setEscrowLockedTokens(
                prepared.tokenAllocation());

        milestoneRepository.save(milestone);
        agreementRepository.save(agreement);

        /*
         * Activation performs a second validation of the funded
         * first-milestone record before making the version ACTIVE.
         */
        agreementVersionService.activateInitialVersion(
                version,
                agreement);

        agreementRepository.save(agreement);
    }


    /**
     * Records successful funding of a later milestone.
     * Unlike initial funding, this must not activate the version again.
     *
     * Call only after the coordinator verifies the actual on-chain escrow.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markNextMilestoneFunded(
            PreparedMilestoneFunding prepared) {

        if (prepared == null) {
            throw new BusinessException(
                    "Milestone funding details are required");
        }

        Negotiation negotiation =
                negotiationRepository.findById(prepared.negotiationId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Negotiation not found"));

        AgreementVersion version =
                versionRepository.findById(prepared.versionId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agreement version not found"));

        ServiceAgreement agreement =
                agreementRepository.findById(prepared.agreementId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agreement not found"));

        AgreementMilestone milestone =
                milestoneRepository.findByIdAndNegotiationIdForUpdate(
                                prepared.milestoneId(),
                                prepared.negotiationId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Milestone not found"));

        if (!version.getNegotiation().getId()
                .equals(prepared.negotiationId())
                || !agreement.getNegotiation().getId()
                .equals(prepared.negotiationId())
                || !negotiation.getId()
                .equals(prepared.negotiationId())) {
            throw new BusinessException(
                    "Funding records do not belong to the expected negotiation");
        }

        if (agreement.getReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE
                || agreement.getActiveVersion() == null
                || !agreement.getActiveVersion().getId()
                .equals(prepared.versionId())
                || version.getStatus() != AgreementVersionStatus.ACTIVE
                || version.getReleaseMode()
                != AgreementReleaseMode.MILESTONE_RELEASE) {
            throw new BusinessException(
                    "The expected agreement version is no longer active");
        }

        if (milestone.getStatus()
                != AgreementMilestoneStatus.FUNDING_PENDING
                || !prepared.escrowId().equals(
                milestone.getBlockchainEscrowId())
                || milestone.getFundingAttemptStartedAt() == null) {
            throw new BusinessException(
                    "The milestone funding state or attempt record does not match");
        }

        List<AgreementVersionMilestone> plan =
                versionMilestoneRepository
                        .findByVersion_IdOrderByMilestoneNumberAsc(
                                version.getId());

        int currentIndex = -1;

        for (int i = 0; i < plan.size(); i++) {
            AgreementVersionMilestone item = plan.get(i);

            if (item.getMilestone() != null
                    && item.getMilestone().getId()
                    .equals(prepared.milestoneId())) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex < 1) {
            throw new BusinessException(
                    "The next milestone is not present at a later position in the active plan");
        }

        AgreementVersionMilestone currentItem =
                plan.get(currentIndex);

        if (!Integer.valueOf(currentIndex + 1).equals(
                currentItem.getMilestoneNumber())
                || currentItem.getTokenAllocation() == null
                || !currentItem.getTokenAllocation().equals(
                prepared.tokenAllocation())) {
            throw new BusinessException(
                    "The funded amount does not match the active milestone plan");
        }

        /*
         * All earlier milestones must already have been released.
         */
        for (int i = 0; i < currentIndex; i++) {
            AgreementMilestone earlier =
                    plan.get(i).getMilestone();

            if (earlier == null
                    || earlier.getStatus()
                    != AgreementMilestoneStatus.RELEASED) {
                throw new BusinessException(
                        "Every earlier milestone must be released before this milestone is funded");
            }
        }

        Instant now = Instant.now();

        milestone.setStatus(AgreementMilestoneStatus.FUNDED);
        milestone.setFundedAt(now);
        milestone.setEscrowLockedTokens(
                prepared.tokenAllocation());

        agreement.setEscrowLockedTokens(
                prepared.tokenAllocation());

        milestoneRepository.save(milestone);
        agreementRepository.save(agreement);
    }

    private String toBytes32Uuid(UUID id) {
        return "0x"
                + "0".repeat(32)
                + id.toString().replace("-", "");
    }

    public record PreparedMilestoneFunding(
            UUID negotiationId,
            UUID versionId,
            UUID agreementId,
            UUID sessionId,
            UUID milestoneId,
            UUID requesterId,
            Integer tokenAllocation,
            String requesterAddress,
            String providerAddress,
            String agreementIdBytes32,
            String sessionIdBytes32,
            String escrowId) {
    }
}