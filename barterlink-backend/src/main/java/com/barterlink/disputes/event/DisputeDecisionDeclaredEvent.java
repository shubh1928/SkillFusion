package com.barterlink.disputes.event;

import java.util.UUID;

public record DisputeDecisionDeclaredEvent(UUID disputeId) {
}