package com.barterlink.agreements.service.impl;

import com.barterlink.agreements.dto.response.AgreementResponseDto;
import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.agreements.repository.AgreementRepository;
import com.barterlink.agreements.service.AgreementService;
import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.negotiation.entity.Negotiation;
import com.barterlink.sessions.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.barterlink.agreements.entity.AgreementVersion;
import com.barterlink.agreements.enums.AgreementReleaseMode;
import com.barterlink.agreements.service.AgreementVersionService;
import com.barterlink.blockchain.escrow.BlockchainEscrowTransactionService;
import com.barterlink.blockchain.permit.BlockchainPermitService;
import com.barterlink.wallet.entity.Wallet;
import com.barterlink.wallet.repository.WalletRepository;
import com.barterlink.sessions.dto.response.SessionResponseDto;
import com.barterlink.wallet.service.WalletService;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgreementServiceImpl implements AgreementService {

    private final AgreementRepository agreementRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final BlockchainEscrowTransactionService blockchainEscrowTransactionService;
    private final BlockchainPermitService blockchainPermitService;
    private final WalletRepository walletRepository;
    private final WalletService walletService;
    private final AgreementVersionService agreementVersionService;

    @Transactional
    @Override
    public AgreementResponseDto createAgreement(
            Negotiation negotiation) {

        agreementRepository.findByNegotiation(negotiation)
                .ifPresent(a -> {
                    throw new BusinessException(
                            "Agreement already exists");
                });

        AgreementVersion initialVersion = null;

        if (negotiation.getProposedExchangeMode()
                == com.barterlink.marketplace.enums.ExchangeMode.DIRECT_BARTER) {

            initialVersion =
                    agreementVersionService.requireReadyInitialVersion(
                            negotiation.getId());

            if (initialVersion.getExchangeMode()
                    != com.barterlink.marketplace.enums.ExchangeMode.DIRECT_BARTER
                    || initialVersion.getReleaseMode() != null
                    || !Integer.valueOf(0).equals(
                    initialVersion.getAgreedTokenAmount())) {

                throw new BusinessException(
                        "The initial Direct Barter version must not require a token release mode or token amount");
            }

        } else if (Boolean.TRUE.equals(
                negotiation.getReleaseModeSelectionRequired())
                && negotiation.getProposedExchangeMode() != null) {

            initialVersion =
                    agreementVersionService.requireReadyInitialVersion(
                            negotiation.getId());

            if (initialVersion.getReleaseMode()
                    != AgreementReleaseMode.DIRECT_RELEASE) {

                throw new BusinessException(
                        "Milestone Release cannot enter the existing full-amount escrow path");
            }

            if (negotiation.getRequesterReleaseMode()
                    != AgreementReleaseMode.DIRECT_RELEASE
                    || negotiation.getProviderReleaseMode()
                    != AgreementReleaseMode.DIRECT_RELEASE) {

                throw new BusinessException(
                        "Both participants must have selected Direct Release");
            }
        }

        ServiceAgreement agreement =
                ServiceAgreement.builder()
                        .negotiation(negotiation)

                        .requester(
                                negotiation.getRequester())

                        .provider(
                                negotiation.getProvider())

                        .finalOfferTitle(
                                initialVersion != null
                                        ? initialVersion.getOfferTitle()
                                        : negotiation.getProposedOfferTitle())

                        .finalOfferDescription(
                                initialVersion != null
                                        ? initialVersion.getOfferDescription()
                                        : negotiation.getProposedOfferDescription())

                        .finalRequestTitle(
                                initialVersion != null
                                        ? initialVersion.getRequestTitle()
                                        : negotiation.getProposedRequestTitle())

                        .finalRequestDescription(
                                initialVersion != null
                                        ? initialVersion.getRequestDescription()
                                        : negotiation.getProposedRequestDescription())

                        .exchangeMode(
                                initialVersion != null
                                        ? initialVersion.getExchangeMode()
                                        : negotiation.getProposedExchangeMode())

                        .releaseMode(
                                initialVersion != null
                                        ? initialVersion.getReleaseMode()
                                        : negotiation.getRequesterReleaseMode())

                        .estimatedHours(
                                initialVersion != null
                                        ? initialVersion.getEstimatedHours()
                                        : negotiation.getProposedEstimatedHours())

                        .agreedTokenAmount(
                                initialVersion != null
                                        ? initialVersion.getAgreedTokenAmount()
                                        : negotiation.getProposedTokenAmount() == null
                                        ? 0
                                        : negotiation.getProposedTokenAmount())

                        .escrowLockedTokens(
                                initialVersion != null
                                        ? initialVersion.getAgreedTokenAmount()
                                        : negotiation.getProposedTokenAmount() == null
                                        ? 0
                                        : negotiation.getProposedTokenAmount())

                        .status(AgreementStatus.ACTIVE)

                        .build();

        /*
         * Save first so the database generates the Agreement UUID.
         */
        ServiceAgreement savedAgreement =
                agreementRepository.save(agreement);

        /*
         * Create the session next so the database generates
         * the Session UUID required by the blockchain escrow.
         */
        SessionResponseDto sessionResponse =
                sessionService.createSession(savedAgreement);

        /*
         * Blockchain escrow is required only for
         * TOKEN_BASED and HYBRID exchanges.
         */
        if (savedAgreement.getExchangeMode() != null
                && savedAgreement.getExchangeMode()
                != com.barterlink.marketplace.enums.ExchangeMode.DIRECT_BARTER) {

            Integer agreedTokens =
                    savedAgreement.getAgreedTokenAmount();

            if (agreedTokens == null || agreedTokens <= 0) {
                throw new BusinessException(
                        "Token amount must be greater than zero for blockchain escrow");
            }

            Wallet requesterWallet =
                    walletRepository.findByUserId(
                                    savedAgreement.getRequester().getId())
                            .orElseThrow(() ->
                                    new BusinessException(
                                            "Requester blockchain wallet not found"));

            Wallet providerWallet =
                    walletRepository.findByUserId(
                                    savedAgreement.getProvider().getId())
                            .orElseThrow(() ->
                                    new BusinessException(
                                            "Provider blockchain wallet not found"));

            if (requesterWallet.getBlockchainAddress() == null
                    || requesterWallet.getBlockchainAddress().isBlank()) {

                throw new BusinessException(
                        "Requester blockchain wallet is not provisioned");
            }

            if (providerWallet.getBlockchainAddress() == null
                    || providerWallet.getBlockchainAddress().isBlank()) {

                throw new BusinessException(
                        "Provider blockchain wallet is not provisioned");
            }

            /*
             * BarterLink stores token amounts as whole BLT values
             * in the application.
             *
             * The deployed BLT contract uses 18 decimals.
             *
             * Example:
             * 1 BLT = 1 * 10^18 base units
             */
            BigInteger blockchainAmount =
                    BigInteger.valueOf(agreedTokens)
                            .multiply(
                                    BigInteger.TEN.pow(18));

            /*
             * The escrow contract expects bytes32 for agreementId and sessionId.
             *
             * A Java UUID contains 16 bytes = 32 hexadecimal characters.
             * Therefore each UUID is deterministically left-padded with
             * 16 zero bytes so that it becomes exactly 32 bytes.
             *
             * The escrowId uniquely identifies the agreement/session pair:
             * Agreement UUID (16 bytes) + Session UUID (16 bytes) = 32 bytes.
             */
            String agreementUuidHex =
                    savedAgreement.getId()
                            .toString()
                            .replace("-", "");

            String sessionUuidHex =
                    sessionResponse.getSessionId()
                            .toString()
                            .replace("-", "");

            String agreementId =
                    "0x"
                            + "0".repeat(32)
                            + agreementUuidHex;

            String sessionId =
                    "0x"
                            + "0".repeat(32)
                            + sessionUuidHex;

            String escrowId =
                    "0x"
                            + agreementUuidHex
                            + sessionUuidHex;

            /*
             * Create a fresh permit using the requester's
             * current on-chain permit nonce.
             */
            BigInteger deadline =
                    BigInteger.valueOf(
                                    System.currentTimeMillis() / 1000)
                            .add(
                                    BigInteger.valueOf(3600));

            BlockchainPermitService.PermitSignature permit =
                    blockchainPermitService.createEscrowPermit(
                            savedAgreement.getRequester().getId(),
                            blockchainAmount,
                            deadline);

            /*
             * Perform the real on-chain LOCK.
             *
             * sendLockWithPermit() already creates and manages
             * the blockchain transaction tracking lifecycle.
             */
            blockchainEscrowTransactionService.sendLockWithPermit(
                    escrowId,
                    requesterWallet.getBlockchainAddress(),
                    providerWallet.getBlockchainAddress(),
                    blockchainAmount,
                    agreementId,
                    sessionId,
                    permit);

            /*
             * The blockchain LOCK has been successfully confirmed.
             *
             * Blockchain remains the authoritative token custody layer.
             * The existing WalletService operation is used only to
             * synchronize the application's SQL wallet mirror and
             * record the application-side LOCKED transaction history.
             */
            walletService.lockTokens(
                    savedAgreement.getRequester().getId(),
                    agreedTokens,
                    "Blockchain escrow locked: " + escrowId);

            /*
             * Persist the blockchain escrow identifier against the Agreement.
             */
            savedAgreement.setBlockchainEscrowId(
                    escrowId);

            savedAgreement =
                    agreementRepository.save(savedAgreement);
        }

        if (initialVersion != null) {

            /*
             * Activate V1 only after the existing Direct Release
             * escrow lock and wallet synchronization have succeeded.
             */
            agreementVersionService.activateInitialVersion(
                    initialVersion,
                    savedAgreement);

            savedAgreement =
                    agreementRepository.save(savedAgreement);
        }

        return mapToResponse(savedAgreement);
    }

    @Override
    public AgreementResponseDto getAgreement(
            UUID agreementId) {

        return mapToResponse(
                getAgreementEntity(agreementId));
    }

    @Override
    public List<AgreementResponseDto> getRequesterAgreements(
            UUID requesterId) {

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Requester not found"));

        return agreementRepository.findByRequester(requester)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AgreementResponseDto> getProviderAgreements(
            UUID providerId) {

        User provider = userRepository.findById(providerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider not found"));

        return agreementRepository.findByProvider(provider)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ServiceAgreement getAgreementEntity(
            UUID agreementId) {

        return agreementRepository.findById(agreementId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Agreement not found"));
    }

    private AgreementResponseDto mapToResponse(
            ServiceAgreement agreement) {

        AgreementResponseDto dto =
                new AgreementResponseDto();

        dto.setAgreementId(
                agreement.getId());

        dto.setNegotiationId(
                agreement.getNegotiation().getId());

        dto.setRequestId(
                agreement.getNegotiation()
                        .getServiceRequest()
                        .getId());

        dto.setRequesterId(
                agreement.getRequester().getId());

        dto.setRequesterName(
                agreement.getRequester().getName());

        dto.setProviderId(
                agreement.getProvider().getId());

        dto.setProviderName(
                agreement.getProvider().getName());

        dto.setEscrowLockedTokens(
                agreement.getEscrowLockedTokens());

        dto.setStatus(
                agreement.getStatus());

        dto.setAcceptedAt(
                agreement.getAcceptedAt());

        dto.setCompletedAt(
                agreement.getCompletedAt());

        dto.setOfferTitle(
                agreement.getFinalOfferTitle());

        dto.setOfferDescription(
                agreement.getFinalOfferDescription());

        dto.setRequestTitle(
                agreement.getFinalRequestTitle());

        dto.setRequestDescription(
                agreement.getFinalRequestDescription());

        dto.setExchangeMode(
                agreement.getExchangeMode());

        dto.setReleaseMode(
                agreement.getReleaseMode());

        dto.setEstimatedHours(
                agreement.getEstimatedHours());

        dto.setAgreedTokenAmount(
                agreement.getAgreedTokenAmount());

        return dto;
    }

}
