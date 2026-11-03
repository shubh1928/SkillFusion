package com.barterlink.blockchain.escrow;

import com.barterlink.blockchain.config.BlockchainProperties;
import com.barterlink.blockchain.permit.BlockchainPermitService;
import com.barterlink.blockchain.security.BlockchainProtocolCredentials;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.utils.Numeric;

import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;

import java.math.BigInteger;
import java.util.List;
import java.time.Instant;

import com.barterlink.blockchain.transaction.BlockchainTransaction;
import com.barterlink.blockchain.transaction.BlockchainTransactionTrackingService;
import com.barterlink.blockchain.transaction.BlockchainTransactionType;
import com.barterlink.wallet.entity.Wallet;
import com.barterlink.wallet.repository.WalletRepository;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;

import com.barterlink.blockchain.token.BlockchainTokenService;

@Service
@RequiredArgsConstructor
public class BlockchainEscrowTransactionService {

    private final Web3j web3j;
    private final BlockchainProperties properties;
    private final BlockchainProtocolCredentials protocolCredentials;
    private final BlockchainTokenService blockchainTokenService;
    private final BlockchainTransactionTrackingService blockchainTransactionTrackingService;
    private final WalletRepository walletRepository;

    public BigInteger estimateLockWithPermitGas(
            String escrowId,
            String requesterAddress,
            String providerAddress,
            BigInteger amount,
            String agreementId,
            String sessionId,
            BlockchainPermitService.PermitSignature permit
    ) {

        validateRequiredValue(escrowId, "Escrow ID");
        validateRequiredValue(requesterAddress, "Requester address");
        validateRequiredValue(providerAddress, "Provider address");
        validateRequiredValue(agreementId, "Agreement ID");
        validateRequiredValue(sessionId, "Session ID");

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Escrow amount must be greater than zero"
            );
        }

        if (permit == null) {
            throw new IllegalArgumentException(
                    "Permit signature is required"
            );
        }

        if (!requesterAddress.equalsIgnoreCase(permit.owner())) {
            throw new IllegalArgumentException(
                    "Permit owner does not match requester address"
            );
        }

        if (!properties.getEscrowAddress()
                .equalsIgnoreCase(permit.spender())) {
            throw new IllegalArgumentException(
                    "Permit spender does not match configured escrow contract"
            );
        }

        if (!amount.equals(permit.amount())) {
            throw new IllegalArgumentException(
                    "Permit amount does not match escrow amount"
            );
        }

        byte[] escrowIdBytes =
                parseBytes32(escrowId, "Escrow ID");

        byte[] agreementIdBytes =
                parseBytes32(agreementId, "Agreement ID");

        byte[] sessionIdBytes =
                parseBytes32(sessionId, "Session ID");

        BigInteger v =
                parseV(permit.v());

        byte[] r =
                parseBytes32(permit.r(), "Permit r");

        byte[] s =
                parseBytes32(permit.s(), "Permit s");

        Credentials credentials =
                protocolCredentials.getCredentials();

        if (!credentials.getAddress()
                .equalsIgnoreCase(properties.getProtocolSignerAddress())) {
            throw new IllegalStateException(
                    "Protocol credentials do not match configured signer"
            );
        }

        List<Type> inputParameters = List.of(
                new Bytes32(escrowIdBytes),
                new Address(requesterAddress),
                new Address(providerAddress),
                new Uint256(amount),
                new Bytes32(agreementIdBytes),
                new Bytes32(sessionIdBytes),
                new Uint256(permit.deadline()),
                new Uint8(v),
                new Bytes32(r),
                new Bytes32(s)
        );

        Function function = new Function(
                "lockWithPermit",
                inputParameters,
                List.of()
        );

        String encodedFunction =
                FunctionEncoder.encode(function);

        try {

            EthGasPrice gasPriceResponse =
                    web3j.ethGasPrice().send();

            if (gasPriceResponse.hasError()) {
                throw new IllegalStateException(
                        "Failed to obtain gas price: "
                                + gasPriceResponse.getError().getMessage()
                );
            }

            BigInteger gasPrice =
                    gasPriceResponse.getGasPrice();

            EthEstimateGas estimateResponse =
                    web3j.ethEstimateGas(
                            Transaction.createFunctionCallTransaction(
                                    credentials.getAddress(),
                                    null,
                                    gasPrice,
                                    null,
                                    properties.getEscrowAddress(),
                                    encodedFunction
                            )
                    ).send();

            if (estimateResponse.hasError()) {
                throw new IllegalStateException(
                        "lockWithPermit gas estimation failed: "
                                + estimateResponse.getError().getMessage()
                );
            }

            BigInteger estimatedGas =
                    estimateResponse.getAmountUsed();

            if (estimatedGas == null
                    || estimatedGas.signum() <= 0) {

                throw new IllegalStateException(
                        "Blockchain returned an invalid gas estimate"
                );
            }

            return estimatedGas;

        } catch (Exception e) {

            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }

            throw new IllegalStateException(
                    "Failed to estimate lockWithPermit gas",
                    e
            );
        }
    }

    public TransactionReceipt sendLockWithPermit(
            String escrowId,
            String requesterAddress,
            String providerAddress,
            BigInteger amount,
            String agreementId,
            String sessionId,
            BlockchainPermitService.PermitSignature permit
    ) {

        validateRequiredValue(escrowId, "Escrow ID");
        validateRequiredValue(requesterAddress, "Requester address");
        validateRequiredValue(providerAddress, "Provider address");
        validateRequiredValue(agreementId, "Agreement ID");
        validateRequiredValue(sessionId, "Session ID");

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Escrow amount must be greater than zero"
            );
        }

        if (permit == null) {
            throw new IllegalArgumentException(
                    "Permit signature is required"
            );
        }

        if (!requesterAddress.equalsIgnoreCase(permit.owner())) {
            throw new IllegalArgumentException(
                    "Permit owner does not match requester address"
            );
        }

        if (!properties.getEscrowAddress()
                .equalsIgnoreCase(permit.spender())) {
            throw new IllegalArgumentException(
                    "Permit spender does not match configured escrow contract"
            );
        }

        if (!amount.equals(permit.amount())) {
            throw new IllegalArgumentException(
                    "Permit amount does not match escrow amount"
            );
        }

        if (permit.deadline() == null
                || permit.deadline().compareTo(
                BigInteger.valueOf(Instant.now().getEpochSecond())
        ) <= 0) {

            throw new IllegalArgumentException(
                    "Permit deadline has expired"
            );
        }

        /*
         * Verify that the permit was created against the current nonce.
         * We do not recreate the permit here; we only reject a stale one.
         */
        BigInteger currentNonce =
                blockchainTokenService.getPermitNonce(requesterAddress);

        if (!currentNonce.equals(permit.nonce())) {
            throw new IllegalStateException(
                    "Permit nonce is stale. Expected nonce "
                            + currentNonce
                            + " but received "
                            + permit.nonce()
            );
        }

        byte[] escrowIdBytes =
                parseBytes32(escrowId, "Escrow ID");

        byte[] agreementIdBytes =
                parseBytes32(agreementId, "Agreement ID");

        byte[] sessionIdBytes =
                parseBytes32(sessionId, "Session ID");

        BigInteger v =
                parseV(permit.v());

        byte[] r =
                parseBytes32(permit.r(), "Permit r");

        byte[] s =
                parseBytes32(permit.s(), "Permit s");

        Credentials credentials =
                protocolCredentials.getCredentials();

        if (!credentials.getAddress()
                .equalsIgnoreCase(properties.getProtocolSignerAddress())) {
            throw new IllegalStateException(
                    "Protocol credentials do not match configured signer"
            );
        }

        Wallet requesterWallet =
                walletRepository.findByBlockchainAddress(
                                requesterAddress.trim()
                        )
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Blockchain wallet not found for requester address: "
                                                + requesterAddress
                                )
                        );

        List<Type> inputParameters = List.of(
                new Bytes32(escrowIdBytes),
                new Address(requesterAddress),
                new Address(providerAddress),
                new Uint256(amount),
                new Bytes32(agreementIdBytes),
                new Bytes32(sessionIdBytes),
                new Uint256(permit.deadline()),
                new Uint8(v),
                new Bytes32(r),
                new Bytes32(s)
        );

        Function function = new Function(
                "lockWithPermit",
                inputParameters,
                List.of()
        );

        String encodedFunction =
                FunctionEncoder.encode(function);

        try {

            EthGasPrice gasPriceResponse =
                    web3j.ethGasPrice().send();

            if (gasPriceResponse.hasError()) {
                throw new IllegalStateException(
                        "Failed to obtain gas price: "
                                + gasPriceResponse.getError().getMessage()
                );
            }

            BigInteger gasPrice =
                    gasPriceResponse.getGasPrice();

            EthEstimateGas estimateResponse =
                    web3j.ethEstimateGas(
                            Transaction.createFunctionCallTransaction(
                                    credentials.getAddress(),
                                    null,
                                    gasPrice,
                                    null,
                                    properties.getEscrowAddress(),
                                    encodedFunction
                            )
                    ).send();

            if (estimateResponse.hasError()) {
                throw new IllegalStateException(
                        "lockWithPermit gas estimation failed: "
                                + estimateResponse.getError().getMessage()
                );
            }

            BigInteger estimatedGas =
                    estimateResponse.getAmountUsed();

            if (estimatedGas == null
                    || estimatedGas.signum() <= 0) {

                throw new IllegalStateException(
                        "Blockchain returned an invalid gas estimate"
                );
            }

            /*
             * Add a 20% safety margin over the estimated gas.
             */
            BigInteger gasLimit =
                    estimatedGas
                            .multiply(BigInteger.valueOf(120))
                            .divide(BigInteger.valueOf(100));

            RawTransactionManager transactionManager =
                    new RawTransactionManager(
                            web3j,
                            credentials,
                            properties.getChainId()
                    );




            EthGetTransactionCount nonceResponse =
                    web3j.ethGetTransactionCount(
                            credentials.getAddress(),
                            DefaultBlockParameterName.PENDING
                    ).send();

            if (nonceResponse.hasError()) {
                throw new IllegalStateException(
                        "Failed to obtain protocol signer nonce for lock: "
                                + nonceResponse.getError().getMessage());
            }

            BigInteger nonce = nonceResponse.getTransactionCount();

            RawTransaction rawTransaction =
                    RawTransaction.createTransaction(
                            nonce,
                            gasPrice,
                            gasLimit,
                            properties.getEscrowAddress(),
                            BigInteger.ZERO,
                            encodedFunction
                    );

            String signedRawTransaction =
                    transactionManager.sign(rawTransaction);

            String knownTransactionHash =
                    Numeric.toHexString(
                            Hash.sha3(
                                    Numeric.hexStringToByteArray(
                                            signedRawTransaction
                                    )
                            )
                    );

            BlockchainTransaction trackingTransaction =
                    blockchainTransactionTrackingService.createRequested(
                            requesterWallet,
                            BlockchainTransactionType.LOCK,
                            amount,
                            escrowId,
                            credentials.getAddress(),
                            properties.getEscrowAddress()
                    );

// Persist the exact hash before broadcasting.
            blockchainTransactionTrackingService.recordSignedTransactionHash(
                    trackingTransaction.getId(),
                    knownTransactionHash
            );

            EthSendTransaction sendResponse;

            try {
                sendResponse =
                        web3j.ethSendRawTransaction(
                                signedRawTransaction
                        ).send();

            } catch (java.io.IOException e) {
                throw new IllegalStateException(
                        "Lock submission outcome is uncertain. Transaction hash: "
                                + knownTransactionHash
                                + ". Do not retry automatically.",
                        e
                );
            }

            if (sendResponse.hasError()) {

                String submissionError =
                        sendResponse.getError() == null
                                ? "Unknown RPC error"
                                : sendResponse.getError().getMessage();

                if (!isAlreadyKnownError(submissionError)) {
                    throw new IllegalStateException(
                            "Lock submission requires reconciliation. Transaction hash: "
                                    + knownTransactionHash
                                    + ". RPC error: " + submissionError);
                }

            } else {

                String returnedHash = sendResponse.getTransactionHash();

                if (returnedHash != null
                        && !returnedHash.isBlank()
                        && !returnedHash.equalsIgnoreCase(knownTransactionHash)) {

                    throw new IllegalStateException(
                            "Returned lock transaction hash does not match "
                                    + "the locally calculated signed transaction hash");
                }
            }

            String transactionHash = knownTransactionHash;

            blockchainTransactionTrackingService.markSubmitted(
                    trackingTransaction.getId(),
                    transactionHash
            );


            /*
             * Wait for confirmation.
             *
             * Important: if this times out, the transaction may still be
             * pending on-chain. We intentionally do not retry here.
             */
            PollingTransactionReceiptProcessor receiptProcessor =
                    new PollingTransactionReceiptProcessor(
                            web3j,
                            1000,
                            60
                    );

            TransactionReceipt receipt =
                    receiptProcessor.waitForTransactionReceipt(
                            transactionHash
                    );

            String receiptStatus = receipt.getStatus();

            if ("0x0".equalsIgnoreCase(receiptStatus)) {

                blockchainTransactionTrackingService.markFailed(
                        trackingTransaction.getId(),
                        "lockWithPermit transaction was mined but failed. "
                                + "Transaction hash: " + transactionHash
                );

                throw new IllegalStateException(
                        "lockWithPermit transaction was mined but failed. "
                                + "Transaction hash: " + transactionHash
                );
            }

            if (!"0x1".equalsIgnoreCase(receiptStatus)) {
                throw new IllegalStateException(
                        "lockWithPermit receipt status is unknown. "
                                + "The transaction remains available for reconciliation. "
                                + "Transaction hash: " + transactionHash
                );
            }

            blockchainTransactionTrackingService.markConfirmed(
                    trackingTransaction.getId()
            );

            return receipt;

        } catch (TransactionException e) {

            throw new IllegalStateException(
                    "lockWithPermit transaction confirmation failed. "
                            + "The transaction may still be pending. "
                            + "Do not retry automatically.",
                    e
            );

        } catch (Exception e) {

            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }

            throw new IllegalStateException(
                    "Failed to submit lockWithPermit transaction",
                    e
            );
        }
    }

    public TransactionReceipt sendRelease(String escrowId) {

        validateRequiredValue(escrowId, "Escrow ID");

        byte[] escrowIdBytes =
                parseBytes32(escrowId, "Escrow ID");

        Credentials credentials =
                protocolCredentials.getCredentials();

        if (!credentials.getAddress()
                .equalsIgnoreCase(properties.getProtocolSignerAddress())) {

            throw new IllegalStateException(
                    "Protocol credentials do not match configured signer"
            );
        }

        List<Type> inputParameters = List.of(
                new Bytes32(escrowIdBytes)
        );

        Function function = new Function(
                "release",
                inputParameters,
                List.of()
        );

        String encodedFunction =
                FunctionEncoder.encode(function);

        try {

            EthGasPrice gasPriceResponse =
                    web3j.ethGasPrice().send();

            if (gasPriceResponse.hasError()) {
                throw new IllegalStateException(
                        "Failed to obtain gas price: "
                                + gasPriceResponse.getError().getMessage()
                );
            }

            BigInteger gasPrice =
                    gasPriceResponse.getGasPrice();

            EthEstimateGas estimateResponse =
                    web3j.ethEstimateGas(
                            Transaction.createFunctionCallTransaction(
                                    credentials.getAddress(),
                                    null,
                                    gasPrice,
                                    null,
                                    properties.getEscrowAddress(),
                                    encodedFunction
                            )
                    ).send();

            if (estimateResponse.hasError()) {
                throw new IllegalStateException(
                        "release gas estimation failed: "
                                + estimateResponse.getError().getMessage()
                );
            }

            BigInteger estimatedGas =
                    estimateResponse.getAmountUsed();

            if (estimatedGas == null
                    || estimatedGas.signum() <= 0) {

                throw new IllegalStateException(
                        "Blockchain returned an invalid release gas estimate"
                );
            }

            /*
             * Add a 20% safety margin over the estimated gas.
             */
            BigInteger gasLimit =
                    estimatedGas
                            .multiply(BigInteger.valueOf(120))
                            .divide(BigInteger.valueOf(100));

            RawTransactionManager transactionManager =
                    new RawTransactionManager(
                            web3j,
                            credentials,
                            properties.getChainId()
                    );

            BlockchainTransaction lockTransaction =
                    blockchainTransactionTrackingService
                            .getTransactionByEscrowIdAndType(
                                    escrowId,
                                    BlockchainTransactionType.LOCK
                            );

            Wallet requesterWallet =
                    lockTransaction.getWallet();

            EthGetTransactionCount nonceResponse =
                    web3j.ethGetTransactionCount(
                            credentials.getAddress(),
                            DefaultBlockParameterName.PENDING
                    ).send();

            if (nonceResponse.hasError()) {
                throw new IllegalStateException(
                        "Failed to obtain protocol signer nonce for release: "
                                + nonceResponse.getError().getMessage());
            }

            BigInteger nonce = nonceResponse.getTransactionCount();

            RawTransaction rawTransaction =
                    RawTransaction.createTransaction(
                            nonce,
                            gasPrice,
                            gasLimit,
                            properties.getEscrowAddress(),
                            BigInteger.ZERO,
                            encodedFunction
                    );

            String signedRawTransaction =
                    transactionManager.sign(rawTransaction);

            String knownTransactionHash =
                    Numeric.toHexString(
                            Hash.sha3(
                                    Numeric.hexStringToByteArray(
                                            signedRawTransaction
                                    )
                            )
                    );

            BlockchainTransaction trackingTransaction =
                    blockchainTransactionTrackingService.createRequested(
                            requesterWallet,
                            BlockchainTransactionType.RELEASE,
                            lockTransaction.getAmount(),
                            escrowId,
                            credentials.getAddress(),
                            properties.getEscrowAddress()
                    );

// Persist the exact hash before attempting network submission.
            blockchainTransactionTrackingService.recordSignedTransactionHash(
                    trackingTransaction.getId(),
                    knownTransactionHash
            );

            EthSendTransaction sendResponse =
                    web3j.ethSendRawTransaction(
                            signedRawTransaction
                    ).send();

            if (sendResponse.hasError()) {

                String submissionError =
                        sendResponse.getError() == null
                                ? "Unknown RPC error"
                                : sendResponse.getError().getMessage();

                if (!isAlreadyKnownError(submissionError)) {
                    // Keep the REQUESTED record and known hash for reconciliation.
                    // Do not automatically resubmit or mark the outcome as FAILED.
                    throw new IllegalStateException(
                            "Release submission outcome requires reconciliation. "
                                    + "Transaction hash: " + knownTransactionHash
                                    + ". RPC error: " + submissionError);
                }
            } else {

                String returnedHash =
                        sendResponse.getTransactionHash();

                if (returnedHash != null
                        && !returnedHash.isBlank()
                        && !returnedHash.equalsIgnoreCase(knownTransactionHash)) {

                    throw new IllegalStateException(
                            "Returned release transaction hash does not match "
                                    + "the locally calculated signed transaction hash");
                }
            }

            String transactionHash = knownTransactionHash;

            blockchainTransactionTrackingService.markSubmitted(
                    trackingTransaction.getId(),
                    transactionHash
            );

            /*
             * Wait for confirmation.
             *
             * If confirmation times out, the transaction may still be
             * pending on-chain. Do not automatically retry.
             */
            PollingTransactionReceiptProcessor receiptProcessor =
                    new PollingTransactionReceiptProcessor(
                            web3j,
                            1000,
                            60
                    );

            TransactionReceipt receipt =
                    receiptProcessor.waitForTransactionReceipt(
                            transactionHash
                    );

            String receiptStatus = receipt.getStatus();

            if ("0x0".equalsIgnoreCase(receiptStatus)) {

                blockchainTransactionTrackingService.markFailed(
                        trackingTransaction.getId(),
                        "release transaction was mined but failed. "
                                + "Transaction hash: " + transactionHash
                );

                throw new IllegalStateException(
                        "release transaction was mined but failed. "
                                + "Transaction hash: " + transactionHash
                );
            }

            if (!"0x1".equalsIgnoreCase(receiptStatus)) {
                throw new IllegalStateException(
                        "Release receipt status is unknown. "
                                + "The transaction remains available for reconciliation. "
                                + "Transaction hash: " + transactionHash
                );
            }

            blockchainTransactionTrackingService.markConfirmed(
                    trackingTransaction.getId()
            );

            return receipt;

        } catch (TransactionException e) {

            throw new IllegalStateException(
                    "release transaction confirmation failed. "
                            + "The transaction may still be pending. "
                            + "Do not retry automatically.",
                    e
            );

        } catch (Exception e) {

            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }

            throw new IllegalStateException(
                    "Failed to submit release transaction",
                    e
            );
        }
    }


    public TransactionReceipt sendRefund(String escrowId) {

        validateRequiredValue(escrowId, "Escrow ID");

        byte[] escrowIdBytes =
                parseBytes32(escrowId, "Escrow ID");

        Credentials credentials =
                protocolCredentials.getCredentials();

        if (!credentials.getAddress()
                .equalsIgnoreCase(properties.getProtocolSignerAddress())) {

            throw new IllegalStateException(
                    "Protocol credentials do not match configured signer"
            );
        }

        List<Type> inputParameters = List.of(
                new Bytes32(escrowIdBytes)
        );

        Function function = new Function(
                "refund",
                inputParameters,
                List.of()
        );

        String encodedFunction =
                FunctionEncoder.encode(function);

        try {

            EthGasPrice gasPriceResponse =
                    web3j.ethGasPrice().send();

            if (gasPriceResponse.hasError()) {
                throw new IllegalStateException(
                        "Failed to obtain gas price: "
                                + gasPriceResponse.getError().getMessage()
                );
            }

            BigInteger gasPrice =
                    gasPriceResponse.getGasPrice();

            EthEstimateGas estimateResponse =
                    web3j.ethEstimateGas(
                            Transaction.createFunctionCallTransaction(
                                    credentials.getAddress(),
                                    null,
                                    gasPrice,
                                    null,
                                    properties.getEscrowAddress(),
                                    encodedFunction
                            )
                    ).send();

            if (estimateResponse.hasError()) {
                throw new IllegalStateException(
                        "refund gas estimation failed: "
                                + estimateResponse.getError().getMessage()
                );
            }

            BigInteger estimatedGas =
                    estimateResponse.getAmountUsed();

            if (estimatedGas == null
                    || estimatedGas.signum() <= 0) {

                throw new IllegalStateException(
                        "Blockchain returned an invalid refund gas estimate"
                );
            }

            /*
             * Add a 20% safety margin over the estimated gas.
             */
            BigInteger gasLimit =
                    estimatedGas
                            .multiply(BigInteger.valueOf(120))
                            .divide(BigInteger.valueOf(100));

            BlockchainTransaction lockTransaction =
                    blockchainTransactionTrackingService
                            .getTransactionByEscrowIdAndType(
                                    escrowId,
                                    BlockchainTransactionType.LOCK
                            );

            Wallet requesterWallet =
                    lockTransaction.getWallet();

            /*
             * Get the pending nonce explicitly so that we can build and sign
             * the exact transaction before broadcasting it.
             */
            EthGetTransactionCount nonceResponse =
                    web3j.ethGetTransactionCount(
                            credentials.getAddress(),
                            DefaultBlockParameterName.PENDING
                    ).send();

            if (nonceResponse.hasError()) {
                throw new IllegalStateException(
                        "Failed to obtain protocol signer nonce: "
                                + nonceResponse.getError().getMessage()
                );
            }

            BigInteger nonce =
                    nonceResponse.getTransactionCount();

            /*
             * Build the exact refund transaction.
             */
            RawTransaction rawTransaction =
                    RawTransaction.createTransaction(
                            nonce,
                            gasPrice,
                            gasLimit,
                            properties.getEscrowAddress(),
                            BigInteger.ZERO,
                            encodedFunction
                    );

            /*
             * Sign before broadcasting.
             * This gives us the exact transaction hash even if the RPC later
             * responds with "already known".
             */
            RawTransactionManager transactionManager =
                    new RawTransactionManager(
                            web3j,
                            credentials,
                            properties.getChainId()
                    );

            String signedRawTransaction =
                    transactionManager.sign(rawTransaction);

            String knownTransactionHash =
                    Numeric.toHexString(
                            Hash.sha3(
                                    Numeric.hexStringToByteArray(
                                            signedRawTransaction
                                    )
                            )
                    );

            BlockchainTransaction trackingTransaction =
                    blockchainTransactionTrackingService.createRequested(
                            requesterWallet,
                            BlockchainTransactionType.REFUND,
                            lockTransaction.getAmount(),
                            escrowId,
                            credentials.getAddress(),
                            properties.getEscrowAddress()
                    );

            blockchainTransactionTrackingService.recordSignedTransactionHash(
                    trackingTransaction.getId(),
                    knownTransactionHash
            );

            String transactionHash = knownTransactionHash;

            try {
                EthSendTransaction sendResponse =
                        web3j.ethSendRawTransaction(
                                signedRawTransaction
                        ).send();

                if (sendResponse.hasError()) {
                    String submissionError =
                            sendResponse.getError() == null
                                    ? "Unknown RPC error"
                                    : sendResponse.getError().getMessage();

                    if (!isAlreadyKnownError(submissionError)) {
                        /*
                         * The exact signed hash has already been persisted.
                         * Keep the transaction available for reconciliation.
                         * Do not automatically resubmit it.
                         */
                        throw new IllegalStateException(
                                "Refund submission outcome requires reconciliation. "
                                        + "Transaction hash: " + knownTransactionHash
                                        + ". RPC error: " + submissionError);
                    }

                    // "Already known" refers to the locally signed transaction.
                    transactionHash = knownTransactionHash;

                } else {
                    String returnedHash = sendResponse.getTransactionHash();

                    if (returnedHash != null
                            && !returnedHash.isBlank()
                            && !returnedHash.equalsIgnoreCase(knownTransactionHash)) {
                        throw new IllegalStateException(
                                "Returned refund transaction hash does not match "
                                        + "the locally calculated signed transaction hash");
                    }

                    transactionHash = knownTransactionHash;
                }

            } catch (java.io.IOException e) {
                /*
                 * A transport error does not prove that the network rejected
                 * the transaction. Preserve REQUESTED plus the recorded hash.
                 */
                throw new IllegalStateException(
                        "Refund submission outcome is uncertain. "
                                + "The transaction may still exist on-chain. "
                                + "Do not retry automatically.",
                        e
                );
            }

            blockchainTransactionTrackingService.markSubmitted(
                    trackingTransaction.getId(),
                    transactionHash
            );

            /*
             * Wait for confirmation.
             *
             * If this times out, the transaction may still be pending.
             * Do not automatically retry.
             */
            PollingTransactionReceiptProcessor receiptProcessor =
                    new PollingTransactionReceiptProcessor(
                            web3j,
                            1000,
                            60
                    );

            TransactionReceipt receipt;

            try {

                receipt =
                        receiptProcessor.waitForTransactionReceipt(
                                transactionHash
                        );

            } catch (TransactionException e) {

                /*
                 * Receipt timeout != blockchain failure.
                 *
                 * Keep the tracking record in SUBMITTED state.
                 */
                throw new IllegalStateException(
                        "refund transaction confirmation failed. "
                                + "The transaction may still be pending. "
                                + "Do not retry automatically.",
                        e
                );
            }

            String receiptStatus = receipt.getStatus();

            if ("0x0".equalsIgnoreCase(receiptStatus)) {

                blockchainTransactionTrackingService.markFailed(
                        trackingTransaction.getId(),
                        "refund transaction was mined but failed. "
                                + "Transaction hash: " + transactionHash
                );

                throw new IllegalStateException(
                        "refund transaction was mined but failed. "
                                + "Transaction hash: " + transactionHash
                );
            }

            if (!"0x1".equalsIgnoreCase(receiptStatus)) {
                throw new IllegalStateException(
                        "Refund receipt status is unknown. "
                                + "The transaction remains available for reconciliation. "
                                + "Transaction hash: " + transactionHash
                );
            }

            blockchainTransactionTrackingService.markConfirmed(
                    trackingTransaction.getId()
            );

            return receipt;
        }
        catch (Exception e) {

            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }

            throw new IllegalStateException(
                    "Failed to submit refund transaction",
                    e
            );
        }
    }

    public EscrowDetails getEscrow(String escrowId) {

        validateRequiredValue(escrowId, "Escrow ID");

        byte[] escrowIdBytes =
                parseBytes32(escrowId, "Escrow ID");

        Function function = new Function(
                "getEscrow",
                List.of(
                        new Bytes32(escrowIdBytes)
                ),
                List.of(
                        new TypeReference<Address>() {},
                        new TypeReference<Address>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Bytes32>() {},
                        new TypeReference<Bytes32>() {},
                        new TypeReference<Uint8>() {},
                        new TypeReference<Uint256>() {},
                        new TypeReference<Uint256>() {}
                )
        );

        String encodedFunction =
                FunctionEncoder.encode(function);

        try {

            org.web3j.protocol.core.methods.response.EthCall response =
                    web3j.ethCall(
                            Transaction.createEthCallTransaction(
                                    null,
                                    properties.getEscrowAddress(),
                                    encodedFunction
                            ),
                            DefaultBlockParameterName.LATEST
                    ).send();

            List<Type> decoded =
                    FunctionReturnDecoder.decode(
                            response.getValue(),
                            function.getOutputParameters()
                    );

            if (decoded.size() != 8) {
                throw new IllegalStateException(
                        "Unable to decode escrow data"
                );
            }

            String requester =
                    (String) decoded.get(0).getValue();

            String provider =
                    (String) decoded.get(1).getValue();

            BigInteger amount =
                    (BigInteger) decoded.get(2).getValue();

            byte[] agreementIdBytes =
                    (byte[]) decoded.get(3).getValue();

            byte[] sessionIdBytes =
                    (byte[]) decoded.get(4).getValue();

            BigInteger status =
                    (BigInteger) decoded.get(5).getValue();

            BigInteger createdAt =
                    (BigInteger) decoded.get(6).getValue();

            BigInteger resolvedAt =
                    (BigInteger) decoded.get(7).getValue();

            return new EscrowDetails(
                    requester,
                    provider,
                    amount,
                    Numeric.toHexString(agreementIdBytes),
                    Numeric.toHexString(sessionIdBytes),
                    status,
                    createdAt,
                    resolvedAt
            );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "Failed to read escrow from blockchain",
                    e
            );
        }
    }

    private boolean isAlreadyKnownError(String message) {

        if (message == null || message.isBlank()) {
            return false;
        }

        return message.toLowerCase().contains("already known");
    }

    private void validateRequiredValue(
            String value,
            String fieldName
    ) {

        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " is required"
            );
        }
    }

    private byte[] parseBytes32(
            String value,
            String fieldName
    ) {

        validateRequiredValue(value, fieldName);

        String clean =
                Numeric.cleanHexPrefix(value);

        if (!clean.matches("(?i)[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    fieldName
                            + " must be exactly 32 bytes "
                            + "(64 hexadecimal characters)"
            );
        }

        byte[] bytes =
                Numeric.hexStringToByteArray(clean);

        if (bytes.length != 32) {
            throw new IllegalArgumentException(
                    fieldName + " must contain exactly 32 bytes"
            );
        }

        return bytes;
    }

    private BigInteger parseV(String value) {

        validateRequiredValue(value, "Permit v");

        String clean =
                Numeric.cleanHexPrefix(value);

        BigInteger v;

        try {
            v = new BigInteger(clean, 16);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "Permit v must be a hexadecimal value",
                    ex
            );
        }

        if (!v.equals(BigInteger.valueOf(27))
                && !v.equals(BigInteger.valueOf(28))) {

            throw new IllegalArgumentException(
                    "Permit v must be 27 or 28"
            );
        }

        return v;
    }

    public record EscrowDetails(
            String requester,
            String provider,
            BigInteger amount,
            String agreementId,
            String sessionId,
            BigInteger status,
            BigInteger createdAt,
            BigInteger resolvedAt
    ) {
    }

}