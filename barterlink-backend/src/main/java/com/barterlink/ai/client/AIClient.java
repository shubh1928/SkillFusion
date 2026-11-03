package com.barterlink.ai.client;

import com.barterlink.ai.dto.RecommendationRequest;
import com.barterlink.ai.dto.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.barterlink.ai.dto.FraudPredictionRequest;
import com.barterlink.ai.dto.FraudPredictionResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIClient {

    private final RestTemplate restTemplate;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    public RecommendationResponse recommend(
            RecommendationRequest request
    ) {

        ResponseEntity<RecommendationResponse> response =
                restTemplate.postForEntity(
                        aiServiceUrl + "/recommend",
                        request,
                        RecommendationResponse.class
                );

        return response.getBody();
    }

    public FraudPredictionResponse predictFraud(
            FraudPredictionRequest request
    ) {

        System.out.println("\nCalling FastAPI...");
        System.out.println(aiServiceUrl + "/fraud/predict");

        ResponseEntity<FraudPredictionResponse> response =
                restTemplate.postForEntity(
                        aiServiceUrl + "/fraud/predict",
                        request,
                        FraudPredictionResponse.class
                );

        System.out.println("\nFastAPI HTTP Status : "
                + response.getStatusCode());

        return response.getBody();
    }

}