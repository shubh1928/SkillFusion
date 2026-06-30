package com.barterlink.sessions.repository;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceSessionRepository
        extends JpaRepository<ServiceSession, UUID> {

    Optional<ServiceSession> findByAgreement(
            ServiceAgreement agreement);

    List<ServiceSession> findByStatus(
            SessionStatus status);

    List<ServiceSession> findByAgreementRequesterId(
            UUID requesterId);

    List<ServiceSession> findByAgreementProviderId(
            UUID providerId);
}