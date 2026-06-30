package com.barterlink.marketplace.service;

import com.barterlink.marketplace.dto.request.CreateServiceRequestDto;
import com.barterlink.marketplace.dto.response.ServiceRequestResponseDto;

import java.util.List;
import java.util.UUID;

public interface MarketplaceService {

    ServiceRequestResponseDto createRequest(
            UUID requesterId,
            CreateServiceRequestDto dto);

    ServiceRequestResponseDto getRequestById(
            UUID requestId);

    List<ServiceRequestResponseDto> getAllOpenRequests();

    List<ServiceRequestResponseDto> getMyRequests(
            UUID requesterId);

    List<ServiceRequestResponseDto> getAssignedRequests(
            UUID providerId);

    ServiceRequestResponseDto acceptRequest(
            UUID requestId,
            UUID providerId);

    void cancelRequest(
            UUID requestId,
            UUID requesterId);
}
