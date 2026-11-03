package com.barterlink.collaboration.controller;

import com.barterlink.collaboration.dto.response.CollaborationRoomResponseDto;
import com.barterlink.collaboration.service.CollaborationRoomService;
import com.barterlink.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/collaboration")
@RequiredArgsConstructor
public class CollaborationController {

    private final CollaborationRoomService collaborationRoomService;

    @GetMapping("/session/{sessionId}")
    public ApiResponse<CollaborationRoomResponseDto> getRoom(
            @PathVariable UUID sessionId) {

        return ApiResponse.success(
                collaborationRoomService.getRoom(sessionId));
    }
}