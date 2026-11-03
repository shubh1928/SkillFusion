package com.barterlink.collaboration.service;

import com.barterlink.collaboration.dto.response.CollaborationRoomResponseDto;
import com.barterlink.sessions.entity.ServiceSession;

import java.util.UUID;

public interface CollaborationRoomService {

    CollaborationRoomResponseDto createRoom(
            UUID sessionId);

    CollaborationRoomResponseDto activateRoom(
            UUID sessionId);

    CollaborationRoomResponseDto closeRoom(
            UUID sessionId);

    CollaborationRoomResponseDto getRoom(
            UUID sessionId);

}