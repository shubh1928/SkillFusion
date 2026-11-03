package com.barterlink.blockchain.security;

import com.barterlink.blockchain.config.BlockchainProperties;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.Keys;

@Component
public class BlockchainProtocolCredentials {

    private final Credentials credentials;

    public BlockchainProtocolCredentials(
            BlockchainProperties properties
    ) {

        String privateKey =
                System.getenv("BLOCKCHAIN_PROTOCOL_PRIVATE_KEY");

        if (privateKey == null || privateKey.isBlank()) {
            throw new IllegalStateException(
                    "BLOCKCHAIN_PROTOCOL_PRIVATE_KEY environment variable is not set"
            );
        }

        try {
            this.credentials =
                    Credentials.create(privateKey.trim());
        } catch (RuntimeException ex) {
            throw new IllegalStateException(
                    "Invalid BLOCKCHAIN_PROTOCOL_PRIVATE_KEY",
                    ex
            );
        }

        String actualAddress =
                Keys.toChecksumAddress(
                        credentials.getAddress()
                );

        String configuredAddress =
                properties.getProtocolSignerAddress();

        if (configuredAddress == null
                || configuredAddress.isBlank()) {
            throw new IllegalStateException(
                    "Protocol signer address is not configured"
            );
        }

        if (!actualAddress.equalsIgnoreCase(configuredAddress.trim())) {
            throw new IllegalStateException(
                    "Protocol signer private key does not match configured protocol signer address"
            );
        }
    }

    public Credentials getCredentials() {
        return credentials;
    }
}