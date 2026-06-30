package com.barterlink.marketplace.repository;

import com.barterlink.auth.entities.User;
import com.barterlink.marketplace.entity.ServiceRequest;
import com.barterlink.marketplace.enums.ServiceRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceRequestRepository
        extends JpaRepository<ServiceRequest, UUID> {

    List<ServiceRequest> findByStatus(
            ServiceRequestStatus status);

    List<ServiceRequest> findByRequester(
            User requester);

    List<ServiceRequest> findBySelectedProvider(
            User provider);

    List<ServiceRequest> findByStatusOrderByCreatedAtDesc(
            ServiceRequestStatus status);

    List<ServiceRequest> findAllByOrderByCreatedAtDesc();
}