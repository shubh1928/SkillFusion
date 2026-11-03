package com.barterlink.ai.behaviour.service;

import java.util.Map;
import java.util.UUID;

public interface BehaviourAnalyticsService {

    Map<String, Double> calculateFeatures(
            UUID userId
    );

}