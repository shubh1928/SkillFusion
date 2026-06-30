package com.barterlink.marketplace.service.impl;

import com.barterlink.agreements.service.AgreementService;
import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.marketplace.dto.request.CreateServiceRequestDto;
import com.barterlink.marketplace.dto.response.ServiceRequestResponseDto;
import com.barterlink.marketplace.entity.ServiceRequest;
import com.barterlink.marketplace.enums.ServiceRequestStatus;
import com.barterlink.marketplace.repository.ServiceRequestRepository;
import com.barterlink.marketplace.service.MarketplaceService;
import com.barterlink.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarketplaceServiceImpl implements MarketplaceService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final AgreementService agreementService;
    private final WalletService walletService;

    @Override
    public ServiceRequestResponseDto createRequest(
            UUID requesterId,
            CreateServiceRequestDto dto) {

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        ServiceRequest request = ServiceRequest.builder()
                .requester(requester)
                .title(dto.getTitle())
                .description(dto.getDescription())
                .estimatedHours(dto.getEstimatedHours())
                .tokenCost(dto.getTokenCost())
                .selectiveRequest(dto.getSelectiveRequest())
                .status(ServiceRequestStatus.OPEN)
                .build();

        return mapToResponse(
                serviceRequestRepository.save(request));
    }

    @Override
    public ServiceRequestResponseDto getRequestById(
            UUID requestId) {

        ServiceRequest request =
                serviceRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Request not found"));

        return mapToResponse(request);
    }

    @Override
    public List<ServiceRequestResponseDto> getAllOpenRequests() {

        return serviceRequestRepository
                .findByStatusOrderByCreatedAtDesc(
                        ServiceRequestStatus.OPEN)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<ServiceRequestResponseDto> getMyRequests(
            UUID requesterId) {

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        return serviceRequestRepository
                .findByRequester(requester)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<ServiceRequestResponseDto> getAssignedRequests(
            UUID providerId) {

        User provider = userRepository.findById(providerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        return serviceRequestRepository
                .findBySelectedProvider(provider)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    @Override
    public ServiceRequestResponseDto acceptRequest(
            UUID requestId,
            UUID providerId) {

        ServiceRequest request =
                serviceRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Request not found"));

        if (request.getStatus() != ServiceRequestStatus.OPEN) {
            throw new BusinessException(
                    "Request is not open");
        }

        if (request.getSelectedProvider() != null) {
            throw new BusinessException(
                    "Provider already assigned");
        }

        User provider = userRepository.findById(providerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Provider not found"));

        if (request.getRequester().getId().equals(providerId)) {
            throw new BusinessException(
                    "You cannot accept your own request");
        }

        walletService.lockTokens(
                request.getRequester().getId(),
                request.getTokenCost(),
                "Escrow lock for request: "
                        + request.getTitle());

        request.setSelectedProvider(provider);
        request.setStatus(ServiceRequestStatus.ACCEPTED);

        ServiceRequest savedRequest =
                serviceRequestRepository.save(request);

        agreementService.createAgreement(
                savedRequest,
                provider);

        return mapToResponse(savedRequest);
    }

    @Override
    public void cancelRequest(
            UUID requestId,
            UUID requesterId) {

        ServiceRequest request =
                serviceRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Request not found"));

        if (!request.getRequester().getId()
                .equals(requesterId)) {

            throw new BusinessException(
                    "You can only cancel your own request");
        }

        if (request.getStatus() != ServiceRequestStatus.OPEN) {
            throw new BusinessException(
                    "Only open requests can be cancelled");
        }

        request.setStatus(ServiceRequestStatus.CANCELLED);

        serviceRequestRepository.save(request);
    }

    private ServiceRequestResponseDto mapToResponse(
            ServiceRequest request) {

        ServiceRequestResponseDto dto =
                new ServiceRequestResponseDto();

        dto.setRequestId(request.getId());

        dto.setRequesterId(
                request.getRequester().getId());

        dto.setRequesterName(
                request.getRequester().getName());

        if (request.getSelectedProvider() != null) {

            dto.setSelectedProviderId(
                    request.getSelectedProvider().getId());

            dto.setSelectedProviderName(
                    request.getSelectedProvider().getName());
        }

        dto.setTitle(request.getTitle());
        dto.setDescription(request.getDescription());
        dto.setEstimatedHours(request.getEstimatedHours());
        dto.setTokenCost(request.getTokenCost());
        dto.setSelectiveRequest(request.getSelectiveRequest());
        dto.setStatus(request.getStatus());
        dto.setCreatedAt(request.getCreatedAt());

        return dto;
    }
}