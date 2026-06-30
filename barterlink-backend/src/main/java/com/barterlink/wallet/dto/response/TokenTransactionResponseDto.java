package com.barterlink.wallet.dto.response;


import com.barterlink.wallet.enums.TransactionType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class TokenTransactionResponseDto {

    private UUID transactionId;

    private TransactionType transactionType;

    private Integer amount;

    private String description;

    private Instant createdAt;
}