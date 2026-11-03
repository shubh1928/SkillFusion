package com.barterlink.agreements.service;

import com.barterlink.agreements.dto.response.AgreementResponseDto;
import com.barterlink.marketplace.entity.ServiceRequest;
import com.barterlink.auth.entities.User;
import com.barterlink.negotiation.entity.Negotiation;

import java.util.List;
import java.util.UUID;

public interface AgreementService {

    AgreementResponseDto createAgreement(
            Negotiation negotiation);

    AgreementResponseDto getAgreement(
            UUID agreementId);

    List<AgreementResponseDto> getRequesterAgreements(
            UUID requesterId);

    List<AgreementResponseDto> getProviderAgreements(
            UUID providerId);

}
