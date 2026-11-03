package com.barterlink.marketplace.service.impl;

import com.barterlink.agreements.service.AgreementService;
import com.barterlink.ai.fraud.repository.UserFraudAssessmentRepository;
import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.marketplace.dto.request.CreateServiceRequestDto;
import com.barterlink.marketplace.dto.response.ServiceRequestResponseDto;
import com.barterlink.marketplace.entity.ServiceRequest;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.marketplace.enums.ServiceRequestStatus;
import com.barterlink.marketplace.repository.ServiceRequestRepository;
import com.barterlink.marketplace.service.MarketplaceService;
import com.barterlink.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.barterlink.marketplace.entity.RequestSkill;
import com.barterlink.marketplace.repository.RequestSkillRepository;
import com.barterlink.skills.entity.Skill;
import com.barterlink.skills.repository.SkillRepository;
import com.barterlink.marketplace.mapper.MarketplaceMapper;
import com.barterlink.ai.dto.TrustAssessmentSummaryDto;
import com.barterlink.ai.fraud.entity.UserFraudAssessment;
import com.barterlink.ai.dto.FraudReasonDto;
import com.barterlink.ai.fraud.entity.UserFraudReason;
import com.barterlink.auth.services.UserActivityService;
import com.barterlink.rewards.enums.RewardQualifyingActivityType;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MarketplaceServiceImpl implements MarketplaceService {

    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final AgreementService agreementService;
    private final WalletService walletService;
    private final SkillRepository skillRepository;
    private final RequestSkillRepository requestSkillRepository;
    private final MarketplaceMapper marketplaceMapper;
    private final UserFraudAssessmentRepository userFraudAssessmentRepository;
    private final UserActivityService userActivityService;

    @Transactional
    @Override
    public ServiceRequestResponseDto createRequest(
            UUID requesterId,
            CreateServiceRequestDto dto) {

        User requester = userRepository.findById(requesterId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        // Prevent deactivated or anonymized accounts from creating requests.
        if (!requester.isEnabled()
                || requester.getInactivityDeactivatedAt() != null
                || requester.getAnonymizedAt() != null) {

            throw new BusinessException(
                    "Disabled accounts cannot create service requests."
            );
        }

        // Validate based on exchange mode
        switch (dto.getExchangeMode()) {

            case DIRECT_BARTER -> {

                if (dto.getOfferTitle() == null || dto.getOfferTitle().isBlank()) {
                    throw new BusinessException("Offer title is required for Direct Barter.");
                }

                if (dto.getOfferDescription() == null || dto.getOfferDescription().isBlank()) {
                    throw new BusinessException("Offer description is required for Direct Barter.");
                }
            }

            case TOKEN_BASED -> {

                if (dto.getTokenAmount() == null || dto.getTokenAmount() <= 0) {
                    throw new BusinessException("Token amount must be greater than zero.");
                }
            }

            case HYBRID -> {

                if (dto.getOfferTitle() == null || dto.getOfferTitle().isBlank()) {
                    throw new BusinessException("Offer title is required for Hybrid exchange.");
                }

                if (dto.getOfferDescription() == null || dto.getOfferDescription().isBlank()) {
                    throw new BusinessException("Offer description is required for Hybrid exchange.");
                }

                if (dto.getTokenAmount() == null || dto.getTokenAmount() <= 0) {
                    throw new BusinessException("Token amount must be greater than zero.");
                }
            }
        }

        // Check the requester's available balance for token-based exchanges.
// Direct Barter does not require BLT.
        if (dto.getExchangeMode() != ExchangeMode.DIRECT_BARTER) {

            Integer availableTokens;

            try {
                availableTokens = walletService
                        .getWallet(requesterId)
                        .getAvailableTokens();

            } catch (ResourceNotFoundException ex) {
                throw new BusinessException(
                        "A wallet is required before creating a token-based or hybrid request");
            }

            if (availableTokens == null
                    || availableTokens < dto.getTokenAmount()) {

                throw new BusinessException(
                        "Insufficient available BLT balance to create this request");
            }
        }

        ServiceRequest request = ServiceRequest.builder()
                .requester(requester)
                .title(dto.getTitle())
                .exchangeMode(dto.getExchangeMode())
                .offerTitle(
                        dto.getExchangeMode() == ExchangeMode.TOKEN_BASED
                                ? null
                                : dto.getOfferTitle()
                )

                .offerDescription(
                        dto.getExchangeMode() == ExchangeMode.TOKEN_BASED
                                ? null
                                : dto.getOfferDescription()
                )
                .requestTitle(dto.getRequestTitle())
                .requestDescription(dto.getRequestDescription())
                .estimatedHours(dto.getEstimatedHours())
                .tokenAmount(dto.getTokenAmount())
                .selectiveRequest(dto.getSelectiveRequest())
                .status(ServiceRequestStatus.OPEN)
                .build();

        ServiceRequest savedRequest =
                serviceRequestRepository.save(request);

        if (dto.getSkillIds() != null &&
                !dto.getSkillIds().isEmpty()) {

            for (UUID skillId : dto.getSkillIds()) {

                Skill skill = skillRepository.findById(skillId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Skill not found"));

                RequestSkill requestSkill =
                        RequestSkill.builder()
                                .request(savedRequest)
                                .skill(skill)
                                .preferred(false)
                                .build();

                requestSkillRepository.save(requestSkill);
            }
        }

        ServiceRequestResponseDto response =
                marketplaceMapper.toResponse(savedRequest);

        enrichFundingEligibility(savedRequest, response);

        userActivityService.recordQualifyingActivity(
                requesterId,
                RewardQualifyingActivityType.MARKETPLACE_REQUEST_CREATED,
                savedRequest.getId().toString()
        );

        return response;
    }

    @Override
    public ServiceRequestResponseDto getRequestById(
            UUID requestId) {

        ServiceRequest request =
                serviceRequestRepository.findById(requestId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Request not found"));

        // Hide open requests owned by deactivated or anonymized accounts.
        if (request.getStatus() == ServiceRequestStatus.OPEN) {
            User requester = request.getRequester();

            if (!requester.isEnabled()
                    || requester.getInactivityDeactivatedAt() != null
                    || requester.getAnonymizedAt() != null) {

                throw new ResourceNotFoundException("Request not found");
            }
        }


        ServiceRequestResponseDto dto =
                marketplaceMapper.toResponse(request);

        enrichFundingEligibility(request, dto);

        enrichTrustAssessment(request, dto);

        return dto;
    }

    @Override
    public List<ServiceRequestResponseDto> getAllOpenRequests() {

        return serviceRequestRepository
                .findByStatusOrderByCreatedAtDesc(
                        ServiceRequestStatus.OPEN)
                .stream()
                .map(request -> {

                    ServiceRequestResponseDto dto =
                            marketplaceMapper.toResponse(request);

                    enrichFundingEligibility(request, dto);

                    enrichTrustAssessment(request, dto);

                    return dto;

                })
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
                .map(request -> {

                    ServiceRequestResponseDto dto =
                            marketplaceMapper.toResponse(request);

                    enrichFundingEligibility(request, dto);

                    enrichTrustAssessment(request, dto);

                    return dto;

                })
                .toList();
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


    private void enrichFundingEligibility(
            ServiceRequest request,
            ServiceRequestResponseDto dto) {

        if (request.getExchangeMode() == ExchangeMode.DIRECT_BARTER) {
            dto.setRequesterHasSufficientTokens(null);
            return;
        }

        Integer requiredTokens = request.getTokenAmount();

        if (requiredTokens == null || requiredTokens <= 0) {
            dto.setRequesterHasSufficientTokens(false);
            return;
        }

        try {
            Integer availableTokens = walletService
                    .getWallet(request.getRequester().getId())
                    .getAvailableTokens();

            dto.setRequesterHasSufficientTokens(
                    availableTokens != null
                            && availableTokens >= requiredTokens);

        } catch (ResourceNotFoundException ex) {
            dto.setRequesterHasSufficientTokens(false);
        }
    }

    private void enrichTrustAssessment(
            ServiceRequest request,
            ServiceRequestResponseDto dto
    ) {

        UserFraudAssessment assessment =
                userFraudAssessmentRepository
                        .findByUser(request.getRequester())
                        .orElse(null);

        if (assessment == null) {
            dto.setTrustAssessment(null);
            return;
        }

        List<FraudReasonDto> reasons =
                assessment.getReasons()
                        .stream()
                        .map(reason -> FraudReasonDto.builder()
                                .feature(reason.getFeature())
                                .value(reason.getValue())
                                .message(reason.getMessage())
                                .build())
                        .toList();

        TrustAssessmentSummaryDto trustAssessment =
                TrustAssessmentSummaryDto.builder()
                        .riskLevel(assessment.getRiskLevel())
                        .trustScore(assessment.getTrustScore())
                        .riskScore(assessment.getRiskScore())
                        .evaluatedAt(assessment.getEvaluatedAt())
                        .reasons(reasons)
                        .build();

        dto.setTrustAssessment(trustAssessment);
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