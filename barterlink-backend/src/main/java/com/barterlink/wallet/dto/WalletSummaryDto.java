package com.barterlink.wallet.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WalletSummaryDto {

    private Integer availableTokens;

    private Integer lockedTokens;

    private Integer totalTokens;
}