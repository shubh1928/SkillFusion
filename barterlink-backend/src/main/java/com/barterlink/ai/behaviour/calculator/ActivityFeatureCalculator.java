package com.barterlink.ai.behaviour.calculator;

import java.util.Map;
import java.util.UUID;

public interface ActivityFeatureCalculator {

    Map<String, Double> calculate(UUID userId);

}