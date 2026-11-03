package com.barterlink.disputes.service;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.enums.AgreementReleaseMode;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.auth.entities.User;
import com.barterlink.blockchain.config.BlockchainProperties;
import com.barterlink.blockchain.transaction.BlockchainTransaction;
import com.barterlink.blockchain.transaction.BlockchainTransactionRepository;
import com.barterlink.blockchain.transaction.BlockchainTransactionStatus;
import com.barterlink.blockchain.transaction.BlockchainTransactionType;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.disputes.dto.response.AnonymousPanelVoteRationaleDto;
import com.barterlink.disputes.dto.response.ParticipantDisputeSummaryDto;
import com.barterlink.disputes.entity.DisputeCase;
import com.barterlink.disputes.entity.DisputeVote;
import com.barterlink.disputes.enums.DisputeCaseStatus;
import com.barterlink.disputes.enums.DisputeDecisionOutcome;
import com.barterlink.disputes.repository.DisputeCaseRepository;
import com.barterlink.disputes.repository.DisputeVoteRepository;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.sessions.enums.SessionStatus;
import com.barterlink.wallet.entity.Wallet;
import com.barterlink.wallet.enums.TransactionType;
import com.barterlink.wallet.repository.TokenTransactionRepository;
import com.barterlink.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DisputeParticipantService {

    private static final int PANEL_SIZE = 5;

    private final DisputeCaseRepository disputeCaseRepository;
    private final DisputeVoteRepository disputeVoteRepository;

    private final BlockchainTransactionRepository
            blockchainTransactionRepository;

    private final BlockchainProperties blockchainProperties;

    private final WalletRepository walletRepository;
    private final TokenTransactionRepository
            tokenTransactionRepository;

    @Transactional(readOnly = true)
    public List<ParticipantDisputeSummaryDto> getMyDisputes(
            UUID currentUserId) {

        if (currentUserId == null) {
            throw new BusinessException(
                    "Authenticated user ID is required");
        }

        return disputeCaseRepository
                .findParticipantCases(currentUserId)
                .stream()
                .map(disputeCase ->
                        toSummary(disputeCase, currentUserId))
                .toList();
    }

    private ParticipantDisputeSummaryDto toSummary(
            DisputeCase disputeCase,
            UUID currentUserId) {

        ServiceAgreement agreement =
                disputeCase.getSession().getAgreement();

        int evidenceVersion =
                disputeCase.getEvidenceVersion() == null
                        ? 0
                        : disputeCase.getEvidenceVersion();

        /*
         * Only votes for the current evidence revision count.
         * Votes from earlier revisions are not included.
         */
        List<DisputeVote> currentVotes =
                disputeVoteRepository
                        .findByDisputeCase_IdAndEvidenceVersionOrderByCreatedAtAsc(
                                disputeCase.getId(),
                                evidenceVersion);

        boolean decisionDeclared =
                disputeCase.getDecisionOutcome() != null;

        Long requesterVotes = null;
        Long providerVotes = null;

        List<AnonymousPanelVoteRationaleDto> rationales = null;

        /*
         * While voting is open, disclose only the total vote count.
         * Do not reveal which side received votes or any vote reasons.
         */
        if (decisionDeclared) {

            requesterVotes = currentVotes.stream()
                    .filter(vote ->
                            vote.getOutcome()
                                    == DisputeDecisionOutcome.REQUESTER_FAVORED)
                    .count();

            providerVotes = currentVotes.stream()
                    .filter(vote ->
                            vote.getOutcome()
                                    == DisputeDecisionOutcome.PROVIDER_FAVORED)
                    .count();

            /*
             * No panelist identity, seat number or timestamp is
             * included in the anonymous rationale response.
             */
            rationales = currentVotes.stream()
                    .map(vote ->
                            AnonymousPanelVoteRationaleDto.builder()
                                    .outcome(vote.getOutcome())
                                    .reason(vote.getReason())
                                    .build())
                    .toList();
        }

        SettlementInfo settlement =
                settlementInfo(disputeCase, agreement);

        User claimant = disputeCase.getClaimant();
        User respondent = disputeCase.getRespondent();

        return ParticipantDisputeSummaryDto.builder()
                .disputeId(disputeCase.getId())
                .sessionId(disputeCase.getSession().getId())
                .agreementId(agreement.getId())

                .currentUserRole(
                        claimant.getId().equals(currentUserId)
                                ? "CLAIMANT"
                                : "RESPONDENT")

                .claimantName(claimant.getName())
                .respondentName(respondent.getName())

                .requesterName(
                        agreement.getRequester().getName())
                .providerName(
                        agreement.getProvider().getName())

                .reason(disputeCase.getReason())

                .status(disputeCase.getStatus())
                .evidenceVersion(evidenceVersion)

                .createdAt(disputeCase.getCreatedAt())
                .panelAssignedAt(disputeCase.getPanelAssignedAt())

                .decisionOutcome(disputeCase.getDecisionOutcome())
                .decisionSummary(disputeCase.getDecisionSummary())
                .decisionDeclaredAt(disputeCase.getDecisionDeclaredAt())

                .panelSize(PANEL_SIZE)
                .votesSubmitted(currentVotes.size())

                .requesterVotes(requesterVotes)
                .providerVotes(providerVotes)
                .anonymousVoteRationales(rationales)

                .exchangeMode(settlement.exchangeMode())
                .releaseMode(settlement.releaseMode())
                .tokenAmount(settlement.tokenAmount())

                .settlementStatus(settlement.status())
                .settlementDirection(settlement.direction())
                .settlementRecipientName(settlement.recipientName())
                .settlementMessage(settlement.message())

                .blockchainTransactionStatus(
                        settlement.transactionStatus())
                .blockchainTransactionHash(
                        settlement.transactionHash())
                .blockchainTransactionUrl(
                        settlement.transactionUrl())

                .build();
    }

    private SettlementInfo settlementInfo(
            DisputeCase disputeCase,
            ServiceAgreement agreement) {

        ExchangeMode exchangeMode = agreement.getExchangeMode();

        String exchangeModeName =
                exchangeMode == null
                        ? null
                        : exchangeMode.name();

        AgreementReleaseMode releaseMode =
                agreement.getReleaseMode();

        String releaseModeName =
                releaseMode == null
                        ? null
                        : releaseMode.name();

        /*
         * Direct Barter does not transfer BLT tokens.
         */
        if (exchangeMode == ExchangeMode.DIRECT_BARTER) {

            return info(
                    exchangeModeName,
                    releaseModeName,
                    null,
                    "NOT_APPLICABLE",
                    null,
                    null,
                    "Direct Barter has no BLT token settlement. "
                            + "The panel decision affects the "
                            + "agreement/session outcome, not a "
                            + "token transfer.",
                    null,
                    null,
                    null);
        }

        if (exchangeMode != ExchangeMode.TOKEN_BASED
                && exchangeMode != ExchangeMode.HYBRID) {

            return info(
                    exchangeModeName,
                    releaseModeName,
                    agreement.getAgreedTokenAmount(),
                    "UNSUPPORTED",
                    null,
                    null,
                    "The exchange mode is not supported by the "
                            + "current dispute token-settlement "
                            + "workflow. No token transfer is "
                            + "reported as complete.",
                    null,
                    null,
                    null);
        }

        Integer tokenAmount =
                agreement.getAgreedTokenAmount();

        /*
         * Preserve the current rule: general dispute settlement
         * must not settle Milestone Release agreements.
         */
        if (releaseMode == AgreementReleaseMode.MILESTONE_RELEASE) {

            return info(
                    exchangeModeName,
                    releaseModeName,
                    tokenAmount,
                    "UNSUPPORTED",
                    null,
                    null,
                    "General dispute settlement is disabled for "
                            + "Milestone Release agreements in the "
                            + "current backend workflow. Do not infer "
                            + "that tokens have moved from the "
                            + "panel decision.",
                    null,
                    null,
                    null);
        }

        if (releaseMode != AgreementReleaseMode.DIRECT_RELEASE) {

            return info(
                    exchangeModeName,
                    releaseModeName,
                    tokenAmount,
                    "UNSUPPORTED",
                    null,
                    null,
                    "The agreement does not use the supported "
                            + "direct escrow release mode. Token "
                            + "settlement is not verified by this "
                            + "workflow.",
                    null,
                    null,
                    null);
        }

        if (disputeCase.getDecisionOutcome() == null) {

            return info(
                    exchangeModeName,
                    releaseModeName,
                    tokenAmount,
                    "AWAITING_DECISION",
                    null,
                    null,
                    "No final panel decision has been declared. "
                            + "The token disposition has not been "
                            + "determined.",
                    null,
                    null,
                    null);
        }

        boolean refundRequester =
                disputeCase.getDecisionOutcome()
                        == DisputeDecisionOutcome.REQUESTER_FAVORED;

        String direction = refundRequester
                ? "Refund to requester"
                : "Release to provider";

        String recipientName = refundRequester
                ? agreement.getRequester().getName()
                : agreement.getProvider().getName();

        BlockchainTransactionType expectedType = refundRequester
                ? BlockchainTransactionType.REFUND
                : BlockchainTransactionType.RELEASE;

        String escrowId =
                agreement.getBlockchainEscrowId();

        if (escrowId == null
                || escrowId.isBlank()
                || tokenAmount == null
                || tokenAmount <= 0) {

            return info(
                    exchangeModeName,
                    releaseModeName,
                    tokenAmount,
                    "UNVERIFIED",
                    direction,
                    recipientName,
                    "The decision identifies the intended recipient, "
                            + "but the escrow ID or agreed token amount "
                            + "is missing. No token transfer can be "
                            + "verified.",
                    null,
                    null,
                    null);
        }

        List<BlockchainTransaction> matchingTransactions =
                blockchainTransactionRepository
                        .findAllByEscrowIdAndTransactionType(
                                escrowId,
                                expectedType);

        BlockchainTransaction transaction =
                matchingTransactions.stream()
                        .max(Comparator.comparing(
                                BlockchainTransaction::getRequestedAt))
                        .orElse(null);

        if (transaction == null) {

            if (disputeCase.getStatus()
                    == DisputeCaseStatus.RESOLVED) {

                return info(
                        exchangeModeName,
                        releaseModeName,
                        tokenAmount,
                        "UNVERIFIED",
                        direction,
                        recipientName,
                        "The case is marked resolved, but no matching "
                                + "tracked " + expectedType.name()
                                + " transaction was found. Treat token "
                                + "settlement as unverified until "
                                + "reconciled.",
                        null,
                        null,
                        null);
            }

            return info(
                    exchangeModeName,
                    releaseModeName,
                    tokenAmount,
                    "PENDING",
                    direction,
                    recipientName,
                    "The final decision identifies the intended "
                            + "disposition, but no matching tracked "
                            + expectedType.name()
                            + " transaction is available yet. Tokens "
                            + "are not reported as transferred.",
                    null,
                    null,
                    null);
        }

        String transactionStatus =
                transaction.getStatus() == null
                        ? null
                        : transaction.getStatus().name();

        String transactionHash =
                transaction.getTransactionHash();

        String transactionUrl =
                buildTransactionUrl(transactionHash);

        if (transaction.getStatus()
                == BlockchainTransactionStatus.FAILED) {

            return info(
                    exchangeModeName,
                    releaseModeName,
                    tokenAmount,
                    "FAILED",
                    direction,
                    recipientName,
                    "The latest tracked " + expectedType.name()
                            + " transaction is marked FAILED. A "
                            + "successful token settlement is not "
                            + "verified; reconciliation is required.",
                    transactionStatus,
                    transactionHash,
                    transactionUrl);
        }

        if (transaction.getStatus()
                != BlockchainTransactionStatus.CONFIRMED) {

            return info(
                    exchangeModeName,
                    releaseModeName,
                    tokenAmount,
                    "PENDING",
                    direction,
                    recipientName,
                    "The tracked " + expectedType.name()
                            + " transaction is "
                            + (transactionStatus == null
                                    ? "not confirmed"
                                    : transactionStatus)
                            + ". Do not treat the token transfer as "
                            + "complete until confirmation and "
                            + "application reconciliation are verified.",
                    transactionStatus,
                    transactionHash,
                    transactionUrl);
        }

        boolean expectedAgreementStatus = refundRequester
                ? agreement.getStatus() == AgreementStatus.REFUNDED
                : agreement.getStatus() == AgreementStatus.COMPLETED;

        boolean expectedSessionStatus = refundRequester
                ? disputeCase.getSession().getStatus()
                        == SessionStatus.CANCELLED
                : disputeCase.getSession().getStatus()
                        == SessionStatus.COMPLETED;

        boolean walletLedgerFinalized =
                walletLedgerFinalized(
                        disputeCase,
                        agreement,
                        refundRequester,
                        tokenAmount);

        boolean applicationFinalized =
                disputeCase.getStatus()
                        == DisputeCaseStatus.RESOLVED
                        && expectedAgreementStatus
                        && expectedSessionStatus
                        && agreement.getEscrowLockedTokens() != null
                        && agreement.getEscrowLockedTokens() == 0
                        && walletLedgerFinalized;

        if (applicationFinalized) {

            String completedAction = refundRequester
                    ? "refunded to the requester"
                    : "released to the provider";

            return info(
                    exchangeModeName,
                    releaseModeName,
                    tokenAmount,
                    "CONFIRMED",
                    direction,
                    recipientName,
                    "Confirmed: " + tokenAmount
                            + " BLT was " + completedAction
                            + ". The matching blockchain transaction "
                            + "is CONFIRMED and the agreement/session "
                            + "settlement state is finalized.",
                    transactionStatus,
                    transactionHash,
                    transactionUrl);
        }

        return info(
                exchangeModeName,
                releaseModeName,
                tokenAmount,
                "BLOCKCHAIN_CONFIRMED",
                direction,
                recipientName,
                "The matching blockchain transaction is CONFIRMED, "
                        + "but the dispute, agreement, session, or "
                        + "wallet finalization state has not reached "
                        + "the expected final state yet. Application "
                        + "reconciliation is not confirmed.",
                transactionStatus,
                transactionHash,
                transactionUrl);
    }

    private boolean walletLedgerFinalized(
            DisputeCase disputeCase,
            ServiceAgreement agreement,
            boolean refundRequester,
            Integer tokenAmount) {

        String baseDescription =
                "Dispute " + disputeCase.getId()
                        + " final panel settlement";

        Wallet requesterWallet =
                walletRepository.findByUserId(
                        agreement.getRequester().getId())
                        .orElse(null);

        if (requesterWallet == null) {
            return false;
        }

        if (refundRequester) {

            return tokenTransactionRepository
                    .findByWalletAndTransactionType(
                            requesterWallet,
                            TransactionType.REFUNDED)
                    .stream()
                    .anyMatch(transaction ->
                            tokenAmount.equals(transaction.getAmount())
                                    && baseDescription.equals(
                                            transaction.getDescription()));
        }

        Wallet providerWallet =
                walletRepository.findByUserId(
                        agreement.getProvider().getId())
                        .orElse(null);

        if (providerWallet == null) {
            return false;
        }

        boolean requesterReleaseRecorded =
                tokenTransactionRepository
                        .findByWalletAndTransactionType(
                                requesterWallet,
                                TransactionType.RELEASED)
                        .stream()
                        .anyMatch(transaction ->
                                tokenAmount.equals(transaction.getAmount())
                                        && ("Transferred to provider: "
                                                + baseDescription)
                                                .equals(transaction.getDescription()));

        boolean providerReceiptRecorded =
                tokenTransactionRepository
                        .findByWalletAndTransactionType(
                                providerWallet,
                                TransactionType.EARNED)
                        .stream()
                        .anyMatch(transaction ->
                                tokenAmount.equals(transaction.getAmount())
                                        && baseDescription.equals(
                                                transaction.getDescription()));

        return requesterReleaseRecorded
                && providerReceiptRecorded;
    }

    private SettlementInfo info(
            String exchangeMode,
            String releaseMode,
            Integer tokenAmount,
            String status,
            String direction,
            String recipientName,
            String message,
            String transactionStatus,
            String transactionHash,
            String transactionUrl) {

        return new SettlementInfo(
                exchangeMode,
                releaseMode,
                tokenAmount,
                status,
                direction,
                recipientName,
                message,
                transactionStatus,
                transactionHash,
                transactionUrl);
    }

    private String buildTransactionUrl(
            String transactionHash) {

        if (transactionHash == null
                || transactionHash.isBlank()) {
            return null;
        }

        /*
         * Your current dev profile configures Sepolia.
         * Do not generate an explorer URL for an unknown network.
         */
        if ("sepolia".equalsIgnoreCase(
                blockchainProperties.getNetwork())) {

            return "https://sepolia.etherscan.io/tx/"
                    + transactionHash;
        }

        return null;
    }

    private record SettlementInfo(
            String exchangeMode,
            String releaseMode,
            Integer tokenAmount,
            String status,
            String direction,
            String recipientName,
            String message,
            String transactionStatus,
            String transactionHash,
            String transactionUrl) {
    }
}