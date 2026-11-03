package com.barterlink.blockchain.token;

import com.barterlink.blockchain.config.BlockchainProperties;
import lombok.RequiredArgsConstructor;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.springframework.stereotype.Service;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.utils.Numeric;
import com.barterlink.rewards.enums.RewardCampaign;
import com.barterlink.blockchain.security.BlockchainProtocolCredentials;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.EthEstimateGas;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.response.PollingTransactionReceiptProcessor;

import com.barterlink.blockchain.transaction.BlockchainTransaction;
import com.barterlink.blockchain.transaction.BlockchainTransactionTrackingService;
import com.barterlink.blockchain.transaction.BlockchainTransactionType;
import com.barterlink.common.constants.WalletConstants;
import com.barterlink.wallet.entity.Wallet;
import org.web3j.crypto.Hash;
import org.web3j.crypto.RawTransaction;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import java.util.Locale;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class BlockchainTokenService {

    private final Web3j web3j;
    private final BlockchainProperties properties;
    private final BlockchainProtocolCredentials protocolCredentials;
    private final BlockchainTransactionTrackingService blockchainTransactionTrackingService;

    public BigInteger getBalance(String walletAddress) {
        Function function = new Function(
                "balanceOf",
                Collections.singletonList(new Address(walletAddress)),
                Collections.singletonList(new TypeReference<Uint256>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        try {
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                            null,
                            properties.getTokenAddress(),
                            encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            List<Type> decoded = FunctionReturnDecoder.decode(
                    response.getValue(),
                    function.getOutputParameters()
            );

            if (decoded.isEmpty()) {
                throw new IllegalStateException("Unable to decode BLT balance");
            }

            return (BigInteger) decoded.get(0).getValue();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to read BLT balance from blockchain",
                    e
            );
        }
    }

    public boolean isSignupBonusClaimed(String walletAddress) {
        Function function = new Function(
                "signupBonusClaimed",
                Collections.singletonList(new Address(walletAddress)),
                Collections.singletonList(new TypeReference<Bool>() {})
        );

        String encodedFunction = FunctionEncoder.encode(function);

        try {
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                            null,
                            properties.getTokenAddress(),
                            encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            List<Type> decoded = FunctionReturnDecoder.decode(
                    response.getValue(),
                    function.getOutputParameters()
            );

            if (decoded.isEmpty()) {
                throw new IllegalStateException(
                        "Unable to decode signup bonus status"
                );
            }

            return (Boolean) decoded.get(0).getValue();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to read signup bonus status from blockchain",
                    e
            );
        }
    }

    public TransactionReceipt mintSignupBonus(Wallet wallet) {

        if (wallet == null) {
            throw new IllegalArgumentException(
                    "Wallet is required"
            );
        }

        String walletAddress =
                wallet.getBlockchainAddress();

        if (walletAddress == null
                || walletAddress.isBlank()) {

            throw new IllegalArgumentException(
                    "Blockchain wallet address is required"
            );
        }

        boolean alreadyClaimed =
                isSignupBonusClaimed(walletAddress);

        if (alreadyClaimed) {
            throw new IllegalStateException(
                    "Signup bonus has already been claimed for this wallet"
            );
        }

        Credentials credentials =
                protocolCredentials.getCredentials();

        BigInteger bonusAmount =
                BigInteger.valueOf(
                        WalletConstants.SIGNUP_BONUS
                ).multiply(
                        BigInteger.TEN.pow(18)
                );

        BlockchainTransaction blockchainTransaction =
                blockchainTransactionTrackingService.createRequested(
                        wallet,
                        BlockchainTransactionType.MINT_SIGNUP_BONUS,
                        bonusAmount,
                        null,
                        credentials.getAddress(),
                        walletAddress
                );

        UUID trackingId =
                blockchainTransaction.getId();

        Function function = new Function(
                "mintSignupBonus",
                Collections.singletonList(
                        new Address(walletAddress)
                ),
                Collections.emptyList()
        );

        String encodedFunction =
                FunctionEncoder.encode(function);

        try {

            EthGasPrice gasPriceResponse =
                    web3j.ethGasPrice().send();

            if (gasPriceResponse.hasError()) {

                String reason =
                        "Failed to obtain gas price: "
                                + gasPriceResponse
                                .getError()
                                .getMessage();

                blockchainTransactionTrackingService.markFailed(
                        trackingId,
                        reason
                );

                throw new IllegalStateException(reason);
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
                                    properties.getTokenAddress(),
                                    encodedFunction
                            )
                    ).send();

            if (estimateResponse.hasError()) {

                String reason =
                        "Failed to estimate mint gas: "
                                + estimateResponse
                                .getError()
                                .getMessage();

                blockchainTransactionTrackingService.markFailed(
                        trackingId,
                        reason
                );

                throw new IllegalStateException(reason);
            }

            BigInteger estimatedGas =
                    estimateResponse.getAmountUsed();

            if (estimatedGas == null
                    || estimatedGas.signum() <= 0) {

                String reason =
                        "Blockchain returned an invalid mint gas estimate";

                blockchainTransactionTrackingService.markFailed(
                        trackingId,
                        reason
                );

                throw new IllegalStateException(reason);
            }

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

            EthSendTransaction response =
                    transactionManager.sendTransaction(
                            gasPrice,
                            gasLimit,
                            properties.getTokenAddress(),
                            encodedFunction,
                            BigInteger.ZERO,
                            false
                    );

            if (response.hasError()) {

                String reason =
                        "BLT mint transaction was rejected: "
                                + response
                                .getError()
                                .getMessage();

                blockchainTransactionTrackingService.markFailed(
                        trackingId,
                        reason
                );

                throw new IllegalStateException(reason);
            }

            String transactionHash =
                    response.getTransactionHash();

            if (transactionHash == null
                    || transactionHash.isBlank()) {

                String reason =
                        "Blockchain did not return a transaction hash";

                blockchainTransactionTrackingService.markFailed(
                        trackingId,
                        reason
                );

                throw new IllegalStateException(reason);
            }

            blockchainTransactionTrackingService.markSubmitted(
                    trackingId,
                    transactionHash
            );

            /*
             * Wait for confirmation.
             *
             * If confirmation times out, the transaction may still
             * be pending on-chain. Keep the SQL record SUBMITTED.
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

            if (!receipt.isStatusOK()) {

                String reason =
                        "BLT mint transaction was mined but failed. "
                                + "Transaction hash: "
                                + transactionHash;

                blockchainTransactionTrackingService.markFailed(
                        trackingId,
                        reason
                );

                throw new IllegalStateException(reason);
            }

            blockchainTransactionTrackingService.markConfirmed(
                    trackingId
            );

            return receipt;

        } catch (IllegalStateException e) {

            throw e;

        } catch (Exception e) {

            /*
             * Once the transaction has been submitted, an exception
             * while waiting for confirmation does NOT automatically
             * mean the transaction failed.
             *
             * The record remains SUBMITTED so it can be reconciled later.
             */
            throw new IllegalStateException(
                    "Failed to mint BLT signup bonus on Sepolia",
                    e
            );
        }
    }

    /**
     * Reads the V2 replay-protection mapping. A true result means the contract
     * has already completed this claim key and minted its fixed campaign amount.
     */
    public boolean isRewardClaimProcessed(String claimKey) {
        String rewardTokenAddress = requireTokenAddress();
        byte[] claimKeyBytes = decodeClaimKey(claimKey);

        Function function = new Function(
                "rewardClaimKeyProcessed",
                Collections.singletonList(new Bytes32(claimKeyBytes)),
                Collections.singletonList(new TypeReference<Bool>() {})
        );

        return readRewardBoolean(
                rewardTokenAddress,
                function,
                "Unable to decode reward claim status",
                "Failed to read reward claim status from the V2 token contract"
        );
    }

    /** Reads the campaign pause guard from the V2 token contract. */
    public boolean isRewardCampaignPaused(RewardCampaign campaign) {
        if (campaign == null) {
            throw new IllegalArgumentException(
                    "Reward campaign is required"
            );
        }

        String rewardTokenAddress = requireTokenAddress();

        Function function = new Function(
                "rewardCampaignPaused",
                Collections.singletonList(
                        new Uint8(
                                BigInteger.valueOf(campaign.getContractCode())
                        )
                ),
                Collections.singletonList(new TypeReference<Bool>() {})
        );

        return readRewardBoolean(
                rewardTokenAddress,
                function,
                "Unable to decode reward campaign pause status",
                "Failed to read reward campaign pause status from the V2 token contract"
        );
    }

    /**
     * Submits a fixed-amount campaign reward mint and returns its tracking ID.
     * It deliberately does not wait for a receipt. Receipt reconciliation and
     * wallet-mirror crediting occur asynchronously after confirmation.
     */
    public UUID submitCampaignReward(
            Wallet wallet,
            RewardCampaign campaign,
            String claimKey
    ) {
        if (wallet == null) {
            throw new IllegalArgumentException("Wallet is required");
        }

        if (campaign == null) {
            throw new IllegalArgumentException(
                    "Reward campaign is required"
            );
        }

        String walletAddress = wallet.getBlockchainAddress();

        if (walletAddress == null || walletAddress.isBlank()) {
            throw new IllegalArgumentException(
                    "Recipient blockchain wallet address is required"
            );
        }

        byte[] claimKeyBytes = decodeClaimKey(claimKey);
        String rewardTokenAddress = requireTokenAddress();
        Credentials credentials = protocolCredentials.getCredentials();

        BigInteger amountWei =
                BigInteger.valueOf(campaign.getTokenAmount())
                        .multiply(BigInteger.TEN.pow(18));

        BlockchainTransaction tracked =
                blockchainTransactionTrackingService.createRequestedReward(
                        wallet,
                        amountWei,
                        credentials.getAddress(),
                        rewardTokenAddress,
                        claimKey
                );

        UUID trackingId = tracked.getId();

        Function function = new Function(
                "mintReward",
                List.of(
                        new Address(walletAddress),
                        new Uint8(
                                BigInteger.valueOf(campaign.getContractCode())
                        ),
                        new Bytes32(claimKeyBytes)
                ),
                Collections.emptyList()
        );

        String encodedFunction = FunctionEncoder.encode(function);

        final BigInteger gasPrice;
        final BigInteger gasLimit;

        try {
            EthGasPrice gasPriceResponse =
                    web3j.ethGasPrice().send();

            if (gasPriceResponse.hasError()) {
                throw new IllegalStateException(
                        "Failed to obtain gas price: "
                                + gasPriceResponse.getError().getMessage()
                );
            }

            gasPrice = gasPriceResponse.getGasPrice();

            EthEstimateGas estimateResponse =
                    web3j.ethEstimateGas(
                            Transaction.createFunctionCallTransaction(
                                    credentials.getAddress(),
                                    null,
                                    gasPrice,
                                    null,
                                    rewardTokenAddress,
                                    encodedFunction
                            )
                    ).send();

            if (estimateResponse.hasError()) {
                throw new IllegalStateException(
                        "Campaign reward mint gas estimation failed: "
                                + estimateResponse.getError().getMessage()
                );
            }

            BigInteger estimatedGas =
                    estimateResponse.getAmountUsed();

            if (estimatedGas == null
                    || estimatedGas.signum() <= 0) {
                throw new IllegalStateException(
                        "Blockchain returned an invalid campaign reward gas estimate"
                );
            }

            gasLimit = estimatedGas
                    .multiply(BigInteger.valueOf(120))
                    .divide(BigInteger.valueOf(100));

        } catch (Exception exception) {
            markRewardTransactionFailed(
                    trackingId,
                    "Campaign reward transaction could not be prepared: "
                            + exception.getMessage()
            );

            throw new IllegalStateException(
                    "Failed to prepare the campaign reward transaction",
                    exception
            );
        }

        /*
         * Sign the exact campaign transaction locally so that its hash
         * can be calculated and saved before any broadcast is attempted.
         */
        final String signedRawTransaction;
        final String knownTransactionHash;

        try {
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
                String nonceError = nonceResponse.getError() == null
                        ? "Unknown RPC error"
                        : nonceResponse.getError().getMessage();

                throw new IllegalStateException(
                        "Failed to obtain protocol signer nonce for campaign reward: "
                                + nonceError
                );
            }

            BigInteger nonce = nonceResponse.getTransactionCount();

            if (nonce == null || nonce.signum() < 0) {
                throw new IllegalStateException(
                        "Blockchain returned an invalid protocol signer nonce"
                );
            }

            RawTransaction rawTransaction =
                    RawTransaction.createTransaction(
                            nonce,
                            gasPrice,
                            gasLimit,
                            rewardTokenAddress,
                            BigInteger.ZERO,
                            encodedFunction
                    );

            signedRawTransaction =
                    transactionManager.sign(rawTransaction);

            knownTransactionHash =
                    Numeric.toHexString(
                            Hash.sha3(
                                    Numeric.hexStringToByteArray(
                                            signedRawTransaction
                                    )
                            )
                    );

        } catch (Exception exception) {
            markRewardTransactionFailed(
                    trackingId,
                    "Campaign reward transaction could not be signed or tracked before broadcast: "
                            + exception.getMessage()
            );

            throw new IllegalStateException(
                    "Failed to sign the campaign reward transaction before broadcast",
                    exception
            );
        }

        /*
         * Save the hash before broadcast. If persistence fails, do not send
         * the signed transaction to the network.
         */
        try {
            blockchainTransactionTrackingService.recordSignedRewardTransaction(
                    trackingId,
                    knownTransactionHash,
                    signedRawTransaction
            );

        } catch (RuntimeException trackingException) {
            throw new IllegalStateException(
                    "Campaign reward transaction was not broadcast because "
                            + "its signed hash could not be persisted. "
                            + "Tracking requires reconciliation. Hash: "
                            + knownTransactionHash,
                    trackingException
            );
        }

        /*
         * Broadcast the exact bytes whose hash has already been persisted.
         */
        final EthSendTransaction sendResponse;

        try {
            sendResponse =
                    web3j.ethSendRawTransaction(
                            signedRawTransaction
                    ).send();

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Campaign reward submission outcome is uncertain. "
                            + "The signed transaction hash was saved before broadcast: "
                            + knownTransactionHash
                            + ". Reconcile this exact transaction before any retry.",
                    exception
            );
        }

        if (sendResponse.hasError()) {
            String submissionError =
                    sendResponse.getError() == null
                            ? "Unknown RPC error"
                            : sendResponse.getError().getMessage();

            /*
             * The node may already know this exact signed transaction.
             */
            if (!isAlreadyKnownError(submissionError)) {
                throw new IllegalStateException(
                        "Campaign reward submission requires reconciliation. "
                                + "The locally calculated hash remains recorded: "
                                + knownTransactionHash
                                + ". RPC error: "
                                + submissionError
                );
            }

        } else {
            String returnedHash = sendResponse.getTransactionHash();

            if (returnedHash != null
                    && !returnedHash.isBlank()
                    && !returnedHash.equalsIgnoreCase(
                    knownTransactionHash
            )) {

                throw new IllegalStateException(
                        "The RPC returned a transaction hash that does not "
                                + "match the locally signed campaign reward transaction. "
                                + "Expected: "
                                + knownTransactionHash
                                + ", returned: "
                                + returnedHash
                );
            }
        }

        /*
         * Use the locally calculated hash that was persisted before broadcast.
         */
        try {
            blockchainTransactionTrackingService.markSubmitted(
                    trackingId,
                    knownTransactionHash
            );

        } catch (RuntimeException trackingException) {
            throw new IllegalStateException(
                    "Campaign reward may have been accepted by the network, "
                            + "but its submission status requires reconciliation. "
                            + "Transaction hash: "
                            + knownTransactionHash,
                    trackingException
            );
        }

        return trackingId;
    }

    private boolean readRewardBoolean(
            String contractAddress,
            Function function,
            String decodeFailure,
            String callFailure
    ) {
        String encodedFunction = FunctionEncoder.encode(function);

        try {
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                            null,
                            contractAddress,
                            encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            if (response.hasError()) {
                throw new IllegalStateException(
                        response.getError().getMessage()
                );
            }

            List<Type> decoded = FunctionReturnDecoder.decode(
                    response.getValue(),
                    function.getOutputParameters()
            );

            if (decoded.isEmpty()) {
                throw new IllegalStateException(decodeFailure);
            }

            return (Boolean) decoded.get(0).getValue();

        } catch (Exception exception) {
            throw new IllegalStateException(callFailure, exception);
        }
    }

    private byte[] decodeClaimKey(String claimKey) {
        if (claimKey == null
                || !claimKey.trim().matches("(?i)^0x[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "Reward claim key must be a valid 32-byte hexadecimal value"
            );
        }

        byte[] decoded =
                Numeric.hexStringToByteArray(claimKey.trim());

        if (decoded.length != 32) {
            throw new IllegalArgumentException(
                    "Reward claim key must be exactly 32 bytes"
            );
        }

        return decoded;
    }

    /**
     * Rewards and ordinary wallet operations must target the same BLT token.
     * Keeping one configured token address prevents campaign rewards from
     * being minted into a second token contract that wallet balances do not use.
     */
    private String requireTokenAddress() {

        String address = properties.getTokenAddress();

        if (address == null
                || !address.trim().matches("(?i)^0x[0-9a-f]{40}$")) {

            throw new IllegalStateException(
                    "The BLT token contract address is not configured. "
                            + "Set BLT_TOKEN_ADDRESS to the active BLT token contract."
            );
        }

        return address.trim();
    }

    private boolean isAlreadyKnownError(String message) {
        return message != null
                && message.toLowerCase(Locale.ROOT)
                .contains("already known");
    }

    private void markRewardTransactionFailed(
            UUID transactionId,
            String reason
    ) {
        try {
            blockchainTransactionTrackingService.markFailed(
                    transactionId,
                    reason
            );
        } catch (RuntimeException trackingException) {
            // Preserve the original transaction preparation error.
        }
    }


    public BigInteger getPermitNonce(String walletAddress) {

        if (walletAddress == null || walletAddress.isBlank()) {
            throw new IllegalArgumentException(
                    "Wallet address is required"
            );
        }

        Function function = new Function(
                "nonces",
                Collections.singletonList(
                        new Address(walletAddress)
                ),
                Collections.singletonList(
                        new TypeReference<Uint256>() {}
                )
        );

        String encodedFunction =
                FunctionEncoder.encode(function);

        try {
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                            null,
                            properties.getTokenAddress(),
                            encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            List<Type> decoded =
                    FunctionReturnDecoder.decode(
                            response.getValue(),
                            function.getOutputParameters()
                    );

            if (decoded.isEmpty()) {
                throw new IllegalStateException(
                        "Unable to decode BLT permit nonce"
                );
            }

            return (BigInteger) decoded.get(0).getValue();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to read BLT permit nonce from blockchain",
                    e
            );
        }
    }

    public byte[] getDomainSeparator() {

        Function function = new Function(
                "DOMAIN_SEPARATOR",
                Collections.emptyList(),
                Collections.singletonList(
                        new TypeReference<Bytes32>() {}
                )
        );

        String encodedFunction =
                FunctionEncoder.encode(function);

        try {
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                            null,
                            properties.getTokenAddress(),
                            encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            List<Type> decoded =
                    FunctionReturnDecoder.decode(
                            response.getValue(),
                            function.getOutputParameters()
                    );

            if (decoded.isEmpty()) {
                throw new IllegalStateException(
                        "Unable to decode BLT domain separator"
                );
            }

            return (byte[]) decoded.get(0).getValue();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to read BLT domain separator from blockchain",
                    e
            );
        }
    }



}