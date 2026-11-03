package com.barterlink.reputation.service;

import java.util.UUID;

public interface ReputationService {

    void recalculateReputation(
            UUID userId);

    double calculateEffectiveSkillValue(
            UUID userId);
}