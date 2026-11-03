
package com.barterlink.sessions.repository;

import com.barterlink.sessions.entity.SessionProofContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SessionProofContentRepository
        extends JpaRepository<SessionProofContent, UUID> {
}
