package com.barterlink.blockchain.permit;

import com.barterlink.blockchain.config.BlockchainProperties;
import com.barterlink.blockchain.security.BlockchainKeyEncryptionService;
import com.barterlink.blockchain.token.BlockchainTokenService;
import com.barterlink.wallet.entity.Wallet;
import com.barterlink.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Sign;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlockchainPermitService {

    private static final String PERMIT_TYPE =
            "Permit(address owner,address spender,uint256 value,uint256 nonce,uint256 deadline)";

    private final WalletRepository walletRepository;
    private final BlockchainKeyEncryptionService encryptionService;
    private final BlockchainTokenService blockchainTokenService;
    private final BlockchainProperties properties;

    public PermitSignature createEscrowPermit(
            UUID userId,
            BigInteger amount,
            BigInteger deadline
    ) {

        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Permit amount must be greater than zero"
            );
        }

        if (deadline == null) {
            throw new IllegalArgumentException(
                    "Permit deadline is required"
            );
        }

        BigInteger currentTimestamp =
                BigInteger.valueOf(Instant.now().getEpochSecond());

        if (deadline.compareTo(currentTimestamp) <= 0) {
            throw new IllegalArgumentException(
                    "Permit deadline must be in the future"
            );
        }

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Wallet not found for user"
                        )
                );

        if (wallet.getBlockchainAddress() == null
                || wallet.getBlockchainAddress().isBlank()) {
            throw new IllegalStateException(
                    "Blockchain wallet address is not available"
            );
        }

        if (wallet.getEncryptedPrivateKey() == null
                || wallet.getEncryptedPrivateKey().isBlank()) {
            throw new IllegalStateException(
                    "Encrypted blockchain private key is not available"
            );
        }

        if (wallet.getPrivateKeyIv() == null
                || wallet.getPrivateKeyIv().isBlank()) {
            throw new IllegalStateException(
                    "Blockchain private key IV is not available"
            );
        }

        String ownerAddress = wallet.getBlockchainAddress();
        String spenderAddress = properties.getEscrowAddress();

        String decryptedPrivateKey = null;

        try {
            decryptedPrivateKey =
                    encryptionService.decrypt(
                            wallet.getEncryptedPrivateKey(),
                            wallet.getPrivateKeyIv()
                    );

            Credentials credentials =
                    Credentials.create(decryptedPrivateKey);

            if (!credentials.getAddress()
                    .equalsIgnoreCase(ownerAddress)) {

                throw new IllegalStateException(
                        "Blockchain private key does not match wallet address"
                );
            }

            BigInteger nonce =
                    blockchainTokenService.getPermitNonce(
                            ownerAddress
                    );

            byte[] domainSeparator =
                    blockchainTokenService.getDomainSeparator();

            if (domainSeparator == null
                    || domainSeparator.length != 32) {

                throw new IllegalStateException(
                        "Invalid BLT domain separator"
                );
            }

            byte[] permitTypeHash =
                    Hash.sha3(
                            PERMIT_TYPE.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            byte[] structHash =
                    Hash.sha3(
                            concat(
                                    permitTypeHash,
                                    encodeAddress(ownerAddress),
                                    encodeAddress(spenderAddress),
                                    encodeUint256(amount),
                                    encodeUint256(nonce),
                                    encodeUint256(deadline)
                            )
                    );

            byte[] digest =
                    Hash.sha3(
                            concat(
                                    new byte[]{0x19, 0x01},
                                    domainSeparator,
                                    structHash
                            )
                    );

            Sign.SignatureData signature =
                    Sign.signMessage(
                            digest,
                            credentials.getEcKeyPair(),
                            false
                    );

            return new PermitSignature(
                    ownerAddress,
                    spenderAddress,
                    amount,
                    nonce,
                    deadline,
                    Numeric.toHexString(signature.getV()),
                    Numeric.toHexString(signature.getR()),
                    Numeric.toHexString(signature.getS()),
                    Numeric.toHexString(digest)
            );

        } catch (Exception e) {

            if (e instanceof IllegalStateException) {
                throw (IllegalStateException) e;
            }

            throw new IllegalStateException(
                    "Failed to create BLT ERC-2612 permit signature",
                    e
            );

        } finally {

            /*
             * Do not retain the decrypted private key after signing.
             */
            decryptedPrivateKey = null;
        }
    }

    private byte[] encodeAddress(String address) {

        byte[] addressBytes =
                Numeric.hexStringToByteArray(address);

        if (addressBytes.length != 20) {
            throw new IllegalArgumentException(
                    "Invalid Ethereum address"
            );
        }

        byte[] encoded = new byte[32];

        System.arraycopy(
                addressBytes,
                0,
                encoded,
                12,
                20
        );

        return encoded;
    }

    private byte[] encodeUint256(BigInteger value) {

        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(
                    "uint256 value cannot be null or negative"
            );
        }

        byte[] valueBytes = value.toByteArray();

        if (valueBytes.length > 32) {

            if (valueBytes.length == 33
                    && valueBytes[0] == 0) {

                valueBytes =
                        Arrays.copyOfRange(
                                valueBytes,
                                1,
                                valueBytes.length
                        );

            } else {
                throw new IllegalArgumentException(
                        "Value does not fit into uint256"
                );
            }
        }

        byte[] encoded = new byte[32];

        System.arraycopy(
                valueBytes,
                0,
                encoded,
                32 - valueBytes.length,
                valueBytes.length
        );

        return encoded;
    }

    private byte[] concat(byte[]... arrays) {

        int totalLength = 0;

        for (byte[] array : arrays) {
            totalLength += array.length;
        }

        ByteBuffer buffer =
                ByteBuffer.allocate(totalLength);

        for (byte[] array : arrays) {
            buffer.put(array);
        }

        return buffer.array();
    }

    public record PermitSignature(
            String owner,
            String spender,
            BigInteger amount,
            BigInteger nonce,
            BigInteger deadline,
            String v,
            String r,
            String s,
            String digest
    ) {
    }
}