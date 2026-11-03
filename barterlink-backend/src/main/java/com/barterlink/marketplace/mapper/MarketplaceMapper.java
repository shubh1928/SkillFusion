package com.barterlink.marketplace.mapper;

import com.barterlink.marketplace.dto.response.ServiceRequestResponseDto;
import com.barterlink.marketplace.entity.ServiceRequest;
import com.barterlink.marketplace.repository.RequestSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MarketplaceMapper {

    private final RequestSkillRepository requestSkillRepository;

    public ServiceRequestResponseDto toResponse(
            ServiceRequest request
    ) {

        ServiceRequestResponseDto dto =
                new ServiceRequestResponseDto();

        dto.setRequestId(request.getId());

        dto.setRequesterId(
                request.getRequester().getId());

        dto.setRequesterName(
                request.getRequester().getName());

        dto.setTitle(request.getTitle());
        dto.setEstimatedHours(request.getEstimatedHours());
        dto.setSelectiveRequest(request.getSelectiveRequest());
        dto.setExchangeMode(
                request.getExchangeMode());

        dto.setOfferTitle(
                request.getOfferTitle());

        dto.setOfferDescription(
                request.getOfferDescription());

        dto.setRequestTitle(
                request.getRequestTitle());

        dto.setRequestDescription(
                request.getRequestDescription());

        dto.setTokenAmount(
                request.getTokenAmount());
        dto.setStatus(request.getStatus());
        dto.setCreatedAt(request.getCreatedAt());

        List<String> skills =
                requestSkillRepository.findByRequest(request)
                        .stream()
                        .map(requestSkill ->
                                requestSkill.getSkill().getName())
                        .toList();

        dto.setRequiredSkills(skills);

        return dto;
    }
}