package com.barterlink.collaboration.repository;

import com.barterlink.collaboration.entity.CollaborationRoom;
import com.barterlink.collaboration.enums.CollaborationRoomStatus;
import com.barterlink.sessions.entity.ServiceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CollaborationRoomRepository
        extends JpaRepository<CollaborationRoom, UUID> {

    Optional<CollaborationRoom> findBySession(
            ServiceSession session);

    List<CollaborationRoom> findByStatus(
            CollaborationRoomStatus status);

}