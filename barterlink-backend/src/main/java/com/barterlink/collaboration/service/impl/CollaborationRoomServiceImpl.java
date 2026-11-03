package com.barterlink.collaboration.service.impl;

import com.barterlink.collaboration.dto.response.CollaborationRoomResponseDto;
import com.barterlink.collaboration.entity.CollaborationRoom;
import com.barterlink.collaboration.enums.CollaborationRoomStatus;
import com.barterlink.collaboration.repository.CollaborationRoomRepository;
import com.barterlink.collaboration.service.CollaborationRoomService;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.repository.ServiceSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CollaborationRoomServiceImpl
        implements CollaborationRoomService {

    private final CollaborationRoomRepository collaborationRoomRepository;

    private final ServiceSessionRepository sessionRepository;

    @Override
    public CollaborationRoomResponseDto createRoom(
            UUID sessionId) {

        ServiceSession session =
                sessionRepository.findById(sessionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Session not found"));

        return collaborationRoomRepository
                .findBySession(session)
                .map(this::mapToResponse)
                .orElseGet(() -> {

                    CollaborationRoom room =
                            CollaborationRoom.builder()
                                    .session(session)
                                    .status(CollaborationRoomStatus.CREATED)
                                    .build();

                    CollaborationRoom savedRoom =
                            collaborationRoomRepository.save(room);

                    return mapToResponse(savedRoom);

                });
    }



    @Override
    public CollaborationRoomResponseDto getRoom(
            UUID sessionId) {

        return mapToResponse(
                getRoomEntity(sessionId));
    }



    @Override
    public CollaborationRoomResponseDto activateRoom(
            UUID sessionId) {

        CollaborationRoom room =
                getRoomEntity(sessionId);

        if (room.getStatus()
                == CollaborationRoomStatus.ACTIVE) {

            return mapToResponse(room);

        }

        room.setStatus(
                CollaborationRoomStatus.ACTIVE);

        if (room.getActivatedAt() == null) {

            room.setActivatedAt(
                    Instant.now());

        }

        CollaborationRoom savedRoom =
                collaborationRoomRepository.save(room);

        return mapToResponse(savedRoom);
    }





    @Override
    public CollaborationRoomResponseDto closeRoom(
            UUID sessionId) {

        CollaborationRoom room =
                getRoomEntity(sessionId);

        if (room.getStatus()
                == CollaborationRoomStatus.CLOSED) {

            return mapToResponse(room);

        }

        room.setStatus(
                CollaborationRoomStatus.CLOSED);

        if (room.getClosedAt() == null) {

            room.setClosedAt(
                    Instant.now());

        }

        CollaborationRoom savedRoom =
                collaborationRoomRepository.save(room);

        return mapToResponse(savedRoom);
    }




    private CollaborationRoom getRoomEntity(
            UUID sessionId) {

        ServiceSession session =
                sessionRepository.findById(sessionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Session not found"));

        return collaborationRoomRepository
                .findBySession(session)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Collaboration room not found"));
    }





    private CollaborationRoomResponseDto mapToResponse(
            CollaborationRoom room) {

        CollaborationRoomResponseDto dto =
                new CollaborationRoomResponseDto();

        dto.setRoomId(
                room.getId());

        dto.setSessionId(
                room.getSession().getId());

        dto.setStatus(
                room.getStatus());

        dto.setCreatedAt(
                room.getCreatedAt());

        dto.setActivatedAt(
                room.getActivatedAt());

        dto.setClosedAt(
                room.getClosedAt());

        return dto;
    }

}