package com.barterlink.blockchain.security;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class BlockchainKeyEncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int AES_KEY_SIZE_BYTES = 32;
    private static final int GCM_IV_SIZE_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public BlockchainKeyEncryptionService() {
        String encodedKey = System.getenv("BLOCKCHAIN_ENCRYPTION_KEY");

        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException(
                    "BLOCKCHAIN_ENCRYPTION_KEY environment variable is not set"
            );
        }

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "BLOCKCHAIN_ENCRYPTION_KEY must be Base64 encoded",
                    ex
            );
        }

        if (keyBytes.length != AES_KEY_SIZE_BYTES) {
            throw new IllegalStateException(
                    "BLOCKCHAIN_ENCRYPTION_KEY must decode to exactly 32 bytes"
            );
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public EncryptedValue encrypt(String plaintext) {

        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("Plaintext cannot be null or blank");
        }

        try {
            byte[] iv = new byte[GCM_IV_SIZE_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] encryptedBytes =
                    cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            return new EncryptedValue(
                    Base64.getEncoder().encodeToString(encryptedBytes),
                    Base64.getEncoder().encodeToString(iv)
            );

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to encrypt blockchain private key",
                    ex
            );
        }
    }

    public String decrypt(String ciphertext, String encodedIv) {

        if (ciphertext == null || ciphertext.isBlank()) {
            throw new IllegalArgumentException("Ciphertext cannot be null or blank");
        }

        if (encodedIv == null || encodedIv.isBlank()) {
            throw new IllegalArgumentException("IV cannot be null or blank");
        }

        try {
            byte[] encryptedBytes =
                    Base64.getDecoder().decode(ciphertext);

            byte[] iv =
                    Base64.getDecoder().decode(encodedIv);

            if (iv.length != GCM_IV_SIZE_BYTES) {
                throw new IllegalArgumentException("Invalid GCM IV length");
            }

            Cipher cipher = Cipher.getInstance(ALGORITHM);

            GCMParameterSpec parameterSpec =
                    new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);

            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedBytes =
                    cipher.doFinal(encryptedBytes);

            return new String(
                    decryptedBytes,
                    StandardCharsets.UTF_8
            );

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to decrypt blockchain private key",
                    ex
            );
        }
    }

    public record EncryptedValue(
            String ciphertext,
            String iv
    ) {
    }
}