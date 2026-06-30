package com.barterlink.agreements.service.impl;

import com.barterlink.agreements.dto.response.AgreementResponseDto;
import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.agreements.repository.AgreementRepository;
import com.barterlink.agreements.service.AgreementService;
import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.marketplace.entity.ServiceRequest;
import com.barterlink.sessions.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgreementServiceImpl implements AgreementService {

    private final AgreementRepository agreementRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;

    @Transactional
    @Override
    public AgreementResponseDto createAgreement(
            ServiceRequest request,
            User provider) {

        agreementRepository.findByServiceRequest(request)
                .ifPresent(a -> {
                    throw new BusinessException(
                            "Agreement already exists");
                });

        ServiceAgreement agreement =
                ServiceAgreement.builder()
                        .serviceRequest(request)
                        .requester(request.getRequester())
                        .provider(provider)
                        .escrowLockedTokens(
                                request.getTokenCost())
                        .status(AgreementStatus.ACTIVE)
                        .build();

        ServiceAgreement savedAgreement =
                agreementRepository.save(agreement);

        sessionService.createSession(savedAgreement);

        return mapToResponse(savedAgreement);
    }

    @Override
    public AgreementResponseDto getAgreement(
            UUID agreementId) {

        return mapToResponse(
                getAgreementEntity(agreementId));
    }

    @Override
    public List<AgreementResponseDto> getRequesterAgreements(
            UUID requesterId) {

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Requester not found"));

        return agreementRepository.findByRequester(requester)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<AgreementResponseDto> getProviderAgreements(
            UUID providerId) {

        User provider = userRepository.findById(providerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider not found"));

        return agreementRepository.findByProvider(provider)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private ServiceAgreement getAgreementEntity(
            UUID agreementId) {

        return agreementRepository.findById(agreementId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Agreement not found"));
    }

    private AgreementResponseDto mapToResponse(
            ServiceAgreement agreement) {

        AgreementResponseDto dto =
                new AgreementResponseDto();

        dto.setAgreementId(
                agreement.getId());

        dto.setRequestId(
                agreement.getServiceRequest().getId());

        dto.setRequestTitle(
                agreement.getServiceRequest().getTitle());

        dto.setRequesterId(
                agreement.getRequester().getId());

        dto.setRequesterName(
                agreement.getRequester().getName());

        dto.setProviderId(
                agreement.getProvider().getId());

        dto.setProviderName(
                agreement.getProvider().getName());

        dto.setEscrowLockedTokens(
                agreement.getEscrowLockedTokens());

        dto.setStatus(
                agreement.getStatus());

        dto.setAcceptedAt(
                agreement.getAcceptedAt());

        dto.setCompletedAt(
                agreement.getCompletedAt());

        return dto;
    }

}
