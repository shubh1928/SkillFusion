package com.barterlink.blockchain.wallet;

import com.barterlink.blockchain.security.BlockchainKeyEncryptionService;
import com.barterlink.wallet.entity.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;

@Service
@RequiredArgsConstructor
public class BlockchainUserCredentialsService {

    private final BlockchainKeyEncryptionService blockchainKeyEncryptionService;

    public Credentials loadCredentials(Wallet wallet) {

        if (wallet == null) {
            throw new IllegalArgumentException("Wallet is required");
        }

        String blockchainAddress = wallet.getBlockchainAddress();

        if (blockchainAddress == null || blockchainAddress.isBlank()) {
            throw new IllegalStateException(
                    "Blockchain wallet address is missing"
            );
        }

        String encryptedPrivateKey =
                wallet.getEncryptedPrivateKey();

        if (encryptedPrivateKey == null
                || encryptedPrivateKey.isBlank()) {

            throw new IllegalStateException(
                    "Encrypted blockchain private key is missing"
            );
        }

        String privateKeyIv =
                wallet.getPrivateKeyIv();

        if (privateKeyIv == null || privateKeyIv.isBlank()) {

            throw new IllegalStateException(
                    "Blockchain private key IV is missing"
            );
        }

        String privateKey =
                blockchainKeyEncryptionService.decrypt(
                        encryptedPrivateKey,
                        privateKeyIv
                );

        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalStateException(
                    "Decrypted blockchain private key is empty"
            );
        }

        final Credentials credentials;

        try {
            credentials = Credentials.create(privateKey);
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "Unable to create blockchain credentials",
                    ex
            );
        }

        String derivedAddress =
                Keys.toChecksumAddress(
                        credentials.getAddress()
                );

        String storedAddress =
                Keys.toChecksumAddress(
                        blockchainAddress.trim()
                );

        if (!derivedAddress.equalsIgnoreCase(storedAddress)) {
            throw new IllegalStateException(
                    "Blockchain private key does not match wallet address"
            );
        }

        return credentials;
    }
}