package com.barterlink.negotiation.service;

import com.barterlink.negotiation.dto.request.SendNegotiationMessageRequestDto;
import com.barterlink.negotiation.dto.request.UpdateNegotiationProposalRequestDto;
import com.barterlink.negotiation.dto.response.NegotiationMessageResponseDto;
import com.barterlink.negotiation.dto.response.NegotiationResponseDto;
import com.barterlink.negotiation.dto.request.SelectAgreementReleaseModeRequestDto;

import java.util.List;
import java.util.UUID;

public interface NegotiationService {

    NegotiationResponseDto openNegotiation(
            UUID interestId);

    NegotiationResponseDto continueChat(
            UUID requestId,
            UUID providerId);

    NegotiationResponseDto getNegotiation(
            UUID negotiationId);

    List<NegotiationResponseDto> getRequestNegotiations(
            UUID requestId);

    List<NegotiationResponseDto> getProviderNegotiations(
            UUID providerId);

    List<NegotiationMessageResponseDto> getMessages(
            UUID negotiationId);

    NegotiationMessageResponseDto sendMessage(
            UUID negotiationId,
            UUID senderId,
            SendNegotiationMessageRequestDto request);

    void addSystemMessage(
            UUID negotiationId,
            String message);

    NegotiationResponseDto updateProposal(
            UUID negotiationId,
            UUID userId,
            UpdateNegotiationProposalRequestDto request);

    NegotiationResponseDto selectAgreementReleaseMode(
            UUID negotiationId,
            UUID userId,
            SelectAgreementReleaseModeRequestDto request);

    NegotiationResponseDto requestSession(
            UUID negotiationId,
            UUID providerId);

    NegotiationResponseDto acceptSessionRequest(
            UUID negotiationId,
            UUID requesterId);

    NegotiationResponseDto rejectSessionRequest(
            UUID negotiationId,
            UUID requesterId);

    NegotiationResponseDto cancelNegotiation(
            UUID negotiationId,
            UUID userId);


}