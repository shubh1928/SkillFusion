package com.barterlink.agreements.repository;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.auth.entities.User;
import com.barterlink.marketplace.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgreementRepository
        extends JpaRepository<ServiceAgreement, UUID> {

    Optional<ServiceAgreement> findByServiceRequest(
            ServiceRequest request);

    List<ServiceAgreement> findByRequester(
            User requester);

    List<ServiceAgreement> findByProvider(
            User provider);

    List<ServiceAgreement> findByStatus(
            AgreementStatus status);
}