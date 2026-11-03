package com.barterlink.marketplace.repository;

import com.barterlink.auth.entities.User;
import com.barterlink.marketplace.entity.ServiceRequest;
import com.barterlink.marketplace.enums.ServiceRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.Collection;

public interface ServiceRequestRepository
        extends JpaRepository<ServiceRequest, UUID> {

    List<ServiceRequest> findByStatus(
            ServiceRequestStatus status);

    List<ServiceRequest> findByRequester(
            User requester);

    @Query("""
    SELECT r
    FROM ServiceRequest r
    JOIN r.requester u
    WHERE r.status = :status
      AND u.enable = true
      AND u.inactivityDeactivatedAt IS NULL
      AND u.anonymizedAt IS NULL
    ORDER BY r.createdAt DESC
    """)
    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(
            @Param("status") ServiceRequestStatus status
    );

    List<ServiceRequest> findAllByOrderByCreatedAtDesc();

    List<ServiceRequest> findByIdIn(
            List<UUID> ids);

    long countByRequester(
            User requester
    );

    long countByRequesterAndCreatedAtAfter(
            User requester,
            Instant createdAt
    );

    long countByRequesterAndStatusIn(
            User requester,
            Collection<ServiceRequestStatus> statuses
    );

}