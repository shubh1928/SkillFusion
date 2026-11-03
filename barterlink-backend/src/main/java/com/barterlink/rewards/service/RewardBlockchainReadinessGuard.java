package com.barterlink.rewards.service;

import com.barterlink.blockchain.config.BlockchainProperties;
import com.barterlink.rewards.enums.RewardCampaign;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Bool;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RewardBlockchainReadinessGuard
        implements ApplicationRunner {

    private final Web3j web3j;
    private final BlockchainProperties properties;

    @Value("${barterlink.rewards.enabled:false}")
    private boolean rewardsEnabled;

    @Value("${barterlink.rewards.scheduler.enabled:false}")
    private boolean schedulerEnabled;

    @Override
    public void run(ApplicationArguments args) {

        if (schedulerEnabled && !rewardsEnabled) {
            throw new IllegalStateException(
                    "Reward scheduling cannot be enabled while "
                            + "barterlink.rewards.enabled is false."
            );
        }

        // Safe development behavior: no RPC calls while both switches are off.
        if (!rewardsEnabled && !schedulerEnabled) {
            return;
        }

        String tokenAddress = requireAddress(
                properties.getTokenAddress(),
                "BLT_TOKEN_ADDRESS"
        );

        String escrowAddress = requireAddress(
                properties.getEscrowAddress(),
                "BLT_ESCROW_ADDRESS"
        );

        String signerAddress = requireAddress(
                properties.getProtocolSignerAddress(),
                "configured protocol signer"
        );

        try {
            BigInteger configuredChainId =
                    BigInteger.valueOf(properties.getChainId());

            BigInteger actualChainId = web3j.ethChainId()
                    .send()
                    .getChainId();

            if (actualChainId == null
                    || !configuredChainId.equals(actualChainId)) {

                throw new IllegalStateException(
                        "Reward processing blocked: RPC chain ID does not "
                                + "match barterlink.blockchain.chain-id."
                );
            }

            requireContractCode(tokenAddress, "BLT token");
            requireContractCode(escrowAddress, "BLT escrow");

            boolean migrationOpen = readBoolean(
                    tokenAddress,
                    new Function(
                            "legacyMigrationOpen",
                            Collections.emptyList(),
                            Collections.singletonList(
                                    new TypeReference<Bool>() {}
                            )
                    ),
                    "legacy migration state"
            );

            if (migrationOpen) {
                throw new IllegalStateException(
                        "Reward processing blocked: V2 legacy migration "
                                + "is still open."
                );
            }

            String onChainSigner = readAddress(
                    tokenAddress,
                    new Function(
                            "protocolSigner",
                            Collections.emptyList(),
                            Collections.singletonList(
                                    new TypeReference<Address>() {}
                            )
                    ),
                    "protocol signer"
            );

            if (!onChainSigner.equalsIgnoreCase(signerAddress)) {
                throw new IllegalStateException(
                        "Reward processing blocked: configured protocol "
                                + "signer does not match the active BLT "
                                + "contract signer."
                );
            }

            String escrowToken = readAddress(
                    escrowAddress,
                    new Function(
                            "token",
                            Collections.emptyList(),
                            Collections.singletonList(
                                    new TypeReference<Address>() {}
                            )
                    ),
                    "escrow token address"
            );

            if (!escrowToken.equalsIgnoreCase(tokenAddress)) {
                throw new IllegalStateException(
                        "Reward processing blocked: escrow is not connected "
                                + "to the configured active BLT token."
                );
            }

            /*
             * Verify that the deployed token exposes the expected V2
             * campaign-pause interface.
             *
             * Individual campaigns may remain paused during staged rollout.
             */
            for (RewardCampaign campaign : RewardCampaign.values()) {
                readBoolean(
                        tokenAddress,
                        new Function(
                                "rewardCampaignPaused",
                                Collections.singletonList(
                                        new Uint8(
                                                BigInteger.valueOf(
                                                        campaign.getContractCode()
                                                )
                                        )
                                ),
                                Collections.singletonList(
                                        new TypeReference<Bool>() {}
                                )
                        ),
                        "pause state for " + campaign.name()
                );
            }

        } catch (IllegalStateException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Reward processing readiness verification failed. "
                            + "Keep rewards and the scheduler disabled "
                            + "until the RPC and deployed contracts "
                            + "are verified.",
                    exception
            );
        }
    }

    private String requireAddress(String value, String label) {

        if (value == null
                || !value.trim().matches("(?i)^0x[0-9a-f]{40}$")) {

            throw new IllegalStateException(
                    "Reward processing blocked: " + label
                            + " is not configured as a valid EVM address."
            );
        }

        return value.trim();
    }

    private void requireContractCode(
            String address,
            String label
    ) throws Exception {

        String code = web3j.ethGetCode(
                address,
                DefaultBlockParameterName.LATEST
        ).send().getCode();

        if (code == null
                || code.isBlank()
                || "0x".equalsIgnoreCase(code)) {

            throw new IllegalStateException(
                    "Reward processing blocked: no deployed code was found "
                            + "for " + label + " at " + address + "."
            );
        }
    }

    private boolean readBoolean(
            String contractAddress,
            Function function,
            String label
    ) throws Exception {

        List<Type> values = readFunction(
                contractAddress,
                function,
                label
        );

        return (Boolean) values.get(0).getValue();
    }

    private String readAddress(
            String contractAddress,
            Function function,
            String label
    ) throws Exception {

        List<Type> values = readFunction(
                contractAddress,
                function,
                label
        );

        return (String) values.get(0).getValue();
    }

    private List<Type> readFunction(
            String contractAddress,
            Function function,
            String label
    ) throws Exception {

        String data = FunctionEncoder.encode(function);

        EthCall response = web3j.ethCall(
                Transaction.createEthCallTransaction(
                        null,
                        contractAddress,
                        data
                ),
                DefaultBlockParameterName.LATEST
        ).send();

        if (response.hasError()) {
            throw new IllegalStateException(
                    "Unable to read " + label + " from contract: "
                            + response.getError().getMessage()
            );
        }

        List<Type> decoded = FunctionReturnDecoder.decode(
                response.getValue(),
                function.getOutputParameters()
        );

        if (decoded.size() != 1) {
            throw new IllegalStateException(
                    "Contract did not return a valid value for "
                            + label + "."
            );
        }

        return decoded;
    }
}