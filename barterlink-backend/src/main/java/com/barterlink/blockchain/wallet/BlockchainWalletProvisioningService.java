package com.barterlink.blockchain.wallet;

import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.wallet.entity.Wallet;
import com.barterlink.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BlockchainWalletProvisioningService {

    private final WalletRepository walletRepository;
    private final BlockchainWalletService blockchainWalletService;

    @Transactional
    public String provisionWallet(UUID userId) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Wallet not found"));

        boolean addressExists =
                wallet.getBlockchainAddress() != null
                        && !wallet.getBlockchainAddress().isBlank();

        boolean encryptedKeyExists =
                wallet.getEncryptedPrivateKey() != null
                        && !wallet.getEncryptedPrivateKey().isBlank();

        boolean ivExists =
                wallet.getPrivateKeyIv() != null
                        && !wallet.getPrivateKeyIv().isBlank();

        /*
         * If all blockchain wallet information already exists,
         * return the existing address instead of generating
         * another wallet.
         */
        if (addressExists && encryptedKeyExists && ivExists) {
            return wallet.getBlockchainAddress();
        }

        /*
         * A partially populated wallet record indicates an
         * inconsistent blockchain wallet state.
         */
        if (addressExists || encryptedKeyExists || ivExists) {
            throw new BusinessException(
                    "Incomplete blockchain wallet information");
        }

        /*
         * Generate a new Ethereum wallet and encrypt its
         * private key immediately.
         */
        BlockchainWalletService.GeneratedWallet generatedWallet =
                blockchainWalletService.generateWallet();

        wallet.setBlockchainAddress(
                generatedWallet.blockchainAddress());

        wallet.setEncryptedPrivateKey(
                generatedWallet.encryptedPrivateKey());

        wallet.setPrivateKeyIv(
                generatedWallet.privateKeyIv());

        walletRepository.save(wallet);

        return generatedWallet.blockchainAddress();
    }
}