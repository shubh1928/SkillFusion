package com.barterlink.wallet.dto.response;

import com.barterlink.blockchain.transaction.BlockchainTransactionStatus;
import com.barterlink.blockchain.transaction.BlockchainTransactionType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class BlockchainTransactionResponseDto {

    private UUID transactionId;

    private BlockchainTransactionType transactionType;

    private BlockchainTransactionStatus status;

    private BigDecimal amount;

    private String transactionHash;

    private String escrowId;

    private String fromAddress;

    private String toAddress;

    private String failureReason;

    private Instant requestedAt;

    private Instant submittedAt;

    private Instant confirmedAt;

    private Instant failedAt;
}