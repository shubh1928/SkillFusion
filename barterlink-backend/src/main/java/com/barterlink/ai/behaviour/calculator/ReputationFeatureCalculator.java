package com.barterlink.ai.behaviour.calculator;

import java.util.Map;
import java.util.UUID;

public interface ReputationFeatureCalculator {

    Map<String, Double> calculate(UUID userId);

}