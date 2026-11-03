package com.barterlink.blockchain.escrow;

import com.barterlink.blockchain.config.BlockchainProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;

import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.abi.datatypes.generated.Uint8;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Numeric;

import java.math.BigInteger;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlockchainEscrowService {

    private final Web3j web3j;
    private final BlockchainProperties properties;

    public String getEscrowTokenAddress() {

        Function function = new Function(
                "token",
                Collections.emptyList(),
                Collections.singletonList(
                        new TypeReference<Address>() {}
                )
        );

        String encodedFunction = FunctionEncoder.encode(function);

        try {
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                            null,
                            properties.getEscrowAddress(),
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
                        "Unable to decode Escrow token address"
                );
            }

            return (String) decoded.get(0).getValue();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to read Escrow token address from blockchain",
                    e
            );
        }
    }

    public String getEscrowProtocolSigner() {

        String tokenAddress = getEscrowTokenAddress();

        Function function = new Function(
                "protocolSigner",
                Collections.emptyList(),
                Collections.singletonList(
                        new TypeReference<Address>() {}
                )
        );

        String encodedFunction = FunctionEncoder.encode(function);

        try {
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                            null,
                            tokenAddress,
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
                        "Unable to decode protocol signer address"
                );
            }

            return (String) decoded.get(0).getValue();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to read protocol signer from blockchain",
                    e
            );
        }
    }

    public EscrowData getEscrow(String escrowId) {

        if (escrowId == null || escrowId.isBlank()) {
            throw new IllegalArgumentException("Escrow ID cannot be null or blank");
        }

        String cleanEscrowId = Numeric.cleanHexPrefix(escrowId);

        if (!cleanEscrowId.matches("(?i)[0-9a-f]{64}")) {
            throw new IllegalArgumentException(
                    "Escrow ID must be exactly 32 bytes (64 hexadecimal characters)"
            );
        }

        Function function = new Function(
                "getEscrow",
                Collections.singletonList(
                        new Bytes32(Numeric.hexStringToByteArray(cleanEscrowId))
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

        String encodedFunction = FunctionEncoder.encode(function);

        try {
            EthCall response = web3j.ethCall(
                    Transaction.createEthCallTransaction(
                            null,
                            properties.getEscrowAddress(),
                            encodedFunction
                    ),
                    DefaultBlockParameterName.LATEST
            ).send();

            List<Type> decoded = FunctionReturnDecoder.decode(
                    response.getValue(),
                    function.getOutputParameters()
            );

            if (decoded.size() != 8) {
                throw new IllegalStateException(
                        "Unable to decode Escrow record"
                );
            }

            return new EscrowData(
                    (String) decoded.get(0).getValue(),
                    (String) decoded.get(1).getValue(),
                    (BigInteger) decoded.get(2).getValue(),
                    (byte[]) decoded.get(3).getValue(),
                    (byte[]) decoded.get(4).getValue(),
                    ((BigInteger) decoded.get(5).getValue()).intValue(),
                    (BigInteger) decoded.get(6).getValue(),
                    (BigInteger) decoded.get(7).getValue()
            );

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to read Escrow from blockchain",
                    e
            );
        }
    }

    public record EscrowData(
            String requester,
            String provider,
            BigInteger amount,
            byte[] agreementId,
            byte[] sessionId,
            int status,
            BigInteger createdAt,
            BigInteger resolvedAt
    ) {}

}