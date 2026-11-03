package com.barterlink.ai.behaviour.calculator;

import java.util.Map;
import java.util.UUID;

public interface ConsistencyFeatureCalculator {

    Map<String, Double> calculate(
            Map<String, Double> features
    );

}