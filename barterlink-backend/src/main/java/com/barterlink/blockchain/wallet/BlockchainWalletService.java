package com.barterlink.blockchain.wallet;

import com.barterlink.blockchain.security.BlockchainKeyEncryptionService;
import org.springframework.stereotype.Service;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;

@Service
public class BlockchainWalletService {

    private final BlockchainKeyEncryptionService encryptionService;

    public BlockchainWalletService(
            BlockchainKeyEncryptionService encryptionService
    ) {
        this.encryptionService = encryptionService;
    }

    public GeneratedWallet generateWallet() {

        try {
            // Generate a completely new Ethereum key pair.
            ECKeyPair keyPair = Keys.createEcKeyPair();

            // Derive the Ethereum address from the public/private key pair.
            String address = Keys.toChecksumAddress(
                    Keys.getAddress(keyPair)
            );

            // Convert the private key to a fixed 64-character hexadecimal string.
            String privateKey = String.format(
                    "%064x",
                    keyPair.getPrivateKey()
            );

            // Encrypt the private key immediately.
            BlockchainKeyEncryptionService.EncryptedValue encrypted =
                    encryptionService.encrypt(privateKey);

            return new GeneratedWallet(
                    address,
                    encrypted.ciphertext(),
                    encrypted.iv()
            );

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to generate blockchain wallet",
                    ex
            );
        }
    }

    public record GeneratedWallet(
            String blockchainAddress,
            String encryptedPrivateKey,
            String privateKeyIv
    ) {
    }
}