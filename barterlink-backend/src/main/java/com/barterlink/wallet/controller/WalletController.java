package com.barterlink.wallet.controller;

import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.blockchain.wallet.BlockchainWalletQueryService;
import com.barterlink.common.response.ApiResponse;
import com.barterlink.wallet.dto.response.TokenTransactionResponseDto;
import com.barterlink.wallet.dto.WalletResponseDto;
import com.barterlink.wallet.dto.WalletSummaryDto;
import com.barterlink.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.barterlink.wallet.dto.response.BlockchainTransactionResponseDto;
import com.barterlink.wallet.dto.response.BlockchainWalletResponseDto;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final CurrentUserUtil currentUserUtil;
    private final BlockchainWalletQueryService blockchainWalletQueryService;

    @GetMapping
    public ApiResponse<WalletResponseDto> getWallet() {

        return ApiResponse.success(
                walletService.getWallet(
                        currentUserUtil.getCurrentUserId())
        );
    }

    @GetMapping("/summary")
    public ApiResponse<WalletSummaryDto> getWalletSummary() {

        return ApiResponse.success(
                walletService.getWalletSummary(
                        currentUserUtil.getCurrentUserId())
        );
    }

    @GetMapping("/transactions")
    public ApiResponse<List<TokenTransactionResponseDto>> getTransactions() {

        return ApiResponse.success(
                walletService.getTransactionHistory(
                        currentUserUtil.getCurrentUserId())
        );
    }

    @GetMapping("/blockchain")
    public ApiResponse<BlockchainWalletResponseDto>
    getBlockchainWallet() {

        return ApiResponse.success(
                blockchainWalletQueryService
                        .getBlockchainWallet(
                                currentUserUtil.getCurrentUserId())
        );
    }

    @GetMapping("/blockchain/transactions")
    public ApiResponse<List<BlockchainTransactionResponseDto>>
    getBlockchainTransactions() {

        return ApiResponse.success(
                blockchainWalletQueryService
                        .getBlockchainTransactionHistory(
                                currentUserUtil.getCurrentUserId())
        );
    }


}