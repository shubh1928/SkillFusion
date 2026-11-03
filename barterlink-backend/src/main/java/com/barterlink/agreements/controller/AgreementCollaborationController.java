
package com.barterlink.agreements.controller;

import com.barterlink.agreements.dto.request.AgreementCollaborationUpdateRequestDto;
import com.barterlink.agreements.dto.response.AgreementCollaborationUpdateResponseDto;
import com.barterlink.agreements.service.AgreementCollaborationUpdateService;
import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AgreementCollaborationController {

    private final AgreementCollaborationUpdateService collaborationUpdateService;
    private final SimpMessagingTemplate messagingTemplate;
    private final CurrentUserUtil currentUserUtil;


    // ---------------------------------------------------------
    // EXISTING VERSION-LEVEL COLLABORATION ENDPOINTS
    // ---------------------------------------------------------

    @GetMapping(
            "/api/v1/negotiations/{negotiationId}/versions/{versionId}/collaboration/updates"
    )
    public ApiResponse<List<AgreementCollaborationUpdateResponseDto>> getUpdates(
            @PathVariable UUID negotiationId,
            @PathVariable UUID versionId) {

        return ApiResponse.success(
                "Agreement collaboration updates retrieved successfully",
                collaborationUpdateService.getUpdates(
                        negotiationId,
                        versionId,
                        currentUserUtil.getCurrentUserId()
                )
        );
    }


    @PostMapping(
            "/api/v1/negotiations/{negotiationId}/versions/{versionId}/collaboration/initialize"
    )
    public ApiResponse<AgreementCollaborationUpdateResponseDto> initializeDocument(
            @PathVariable UUID negotiationId,
            @PathVariable UUID versionId,
            @RequestBody AgreementCollaborationUpdateRequestDto request) {

        AgreementCollaborationUpdateResponseDto initialUpdate =
                collaborationUpdateService.initializeDocument(
                        negotiationId,
                        versionId,
                        currentUserUtil.getCurrentUserId(),
                        request
                );

        String topic = "/topic/agreement/"
                + negotiationId
                + "/"
                + versionId;

        messagingTemplate.convertAndSend(topic, initialUpdate);

        return ApiResponse.success(
                "Agreement collaboration document initialized",
                initialUpdate
        );
    }


    // ---------------------------------------------------------
    // NEW NEGOTIATION-LEVEL COLLABORATION ENDPOINTS
    // ---------------------------------------------------------

    @GetMapping(
            "/api/v1/negotiations/{negotiationId}/collaboration/updates"
    )
    public ApiResponse<List<AgreementCollaborationUpdateResponseDto>>
    getNegotiationUpdates(
            @PathVariable UUID negotiationId) {

        return ApiResponse.success(
                "Shared agreement draft updates retrieved successfully",
                collaborationUpdateService.getNegotiationUpdates(
                        negotiationId,
                        currentUserUtil.getCurrentUserId()
                )
        );
    }


    @PostMapping(
            "/api/v1/negotiations/{negotiationId}/collaboration/initialize"
    )
    public ApiResponse<AgreementCollaborationUpdateResponseDto>
    initializeNegotiationDocument(
            @PathVariable UUID negotiationId,
            @RequestBody AgreementCollaborationUpdateRequestDto request) {

        AgreementCollaborationUpdateResponseDto initialUpdate =
                collaborationUpdateService.initializeNegotiationDocument(
                        negotiationId,
                        currentUserUtil.getCurrentUserId(),
                        request
                );

        String topic = "/topic/agreement-draft/" + negotiationId;

        messagingTemplate.convertAndSend(topic, initialUpdate);

        return ApiResponse.success(
                "Shared agreement draft document initialized",
                initialUpdate
        );
    }
}
