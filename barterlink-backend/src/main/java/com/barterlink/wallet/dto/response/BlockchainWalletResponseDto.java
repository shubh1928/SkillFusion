package com.barterlink.wallet.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class BlockchainWalletResponseDto {

    private UUID walletId;

    private String blockchainAddress;

    private long chainId;

    private String tokenContractAddress;

    private String escrowContractAddress;

    private BigDecimal onChainBalance;

    private BigDecimal lockedInEscrow;

    private BigDecimal totalBalance;
}