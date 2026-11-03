package com.barterlink.ai.controller;

import com.barterlink.ai.dto.FraudAssessmentDto;
import com.barterlink.ai.service.FraudAssessmentService;
import com.barterlink.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class FraudAssessmentController {

    private final FraudAssessmentService fraudAssessmentService;

    @GetMapping("/fraud-assessment/{userId}")
    public ApiResponse<FraudAssessmentDto> getAssessment(
            @PathVariable UUID userId
    ) {

        FraudAssessmentDto response =
                fraudAssessmentService.getLatestAssessment(userId);

        return ApiResponse.success(response);
    }
}