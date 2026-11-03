package com.barterlink.ai.controller;

import com.barterlink.ai.client.AIClient;
import com.barterlink.ai.dto.*;
import com.barterlink.common.response.ApiResponse;
import com.barterlink.ai.service.TrustAssessmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AITestController {

    private final AIClient aiClient;
    private final TrustAssessmentService trustAssessmentService;


    @GetMapping("/api/v1/ai/test")
    public ApiResponse<RecommendationResponse> test() {

        RecommendationRequest request =
                RecommendationRequest.builder()
                        .skills(List.of(
                                "React",
                                "Java",
                                "Spring Boot"
                        ))

                        .requests(List.of(

                                RequestData.builder()
                                        .requestId("1")
                                        .title("Need Frontend Developer")
                                        .description("Looking for a React frontend developer.")
                                        .build(),

                                RequestData.builder()
                                        .requestId("2")
                                        .title("Need Database Administrator")
                                        .description("Need MySQL Expert")
                                        .build(),

                                RequestData.builder()
                                        .requestId("3")
                                        .title("Need Java Backend Developer")
                                        .description("Looking for a Spring Boot backend developer.")
                                        .build()

                        ))

                        .build();

        RecommendationResponse response =
                aiClient.recommend(request);

        return ApiResponse.success(response);
    }


    @GetMapping("/api/v1/ai/fraud-test/{userId}")
    public ApiResponse<Void> testFraud(
            @PathVariable UUID userId
    ) {

        trustAssessmentService.refreshAssessment(userId);

        return ApiResponse.success(
                "Trust assessment refreshed successfully."
        );
    }

}