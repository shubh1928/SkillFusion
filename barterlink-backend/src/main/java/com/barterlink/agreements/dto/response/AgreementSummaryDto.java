package com.barterlink.agreements.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgreementSummaryDto {

    private Long activeAgreements;

    private Long completedAgreements;

    private Long disputedAgreements;

    private Long cancelledAgreements;
}