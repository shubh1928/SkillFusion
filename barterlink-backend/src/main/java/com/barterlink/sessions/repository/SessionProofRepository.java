package com.barterlink.sessions.repository;

import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.entity.SessionProof;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionProofRepository
        extends JpaRepository<SessionProof, UUID> {

    List<SessionProof> findBySessionOrderByCreatedAtAsc(
            ServiceSession session);
}