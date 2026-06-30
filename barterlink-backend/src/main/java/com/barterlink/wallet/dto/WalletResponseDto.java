package com.barterlink.wallet.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class WalletResponseDto {

    private UUID walletId;

    private Integer availableTokens;

    private Integer lockedTokens;
}