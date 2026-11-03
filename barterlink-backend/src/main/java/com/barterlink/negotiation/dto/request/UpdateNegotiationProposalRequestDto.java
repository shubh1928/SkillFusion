package com.barterlink.negotiation.dto.request;

import com.barterlink.marketplace.enums.ExchangeMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateNegotiationProposalRequestDto {

    @NotBlank(message = "Offer title is required")
    private String offerTitle;

    @NotBlank(message = "Offer description is required")
    private String offerDescription;

    @NotBlank(message = "Request title is required")
    private String requestTitle;

    @NotBlank(message = "Request description is required")
    private String requestDescription;

    @NotNull(message = "Exchange mode is required")
    private ExchangeMode exchangeMode;

    @NotNull(message = "Estimated hours are required")
    private Integer estimatedHours;

    private Integer tokenAmount;
}