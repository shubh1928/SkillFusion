package com.barterlink.agreements.dto.request;

import com.barterlink.marketplace.enums.ExchangeMode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CreateAgreementVersionRequestDto {

    @NotNull
    private ExchangeMode exchangeMode;

    @Size(max = 255)
    private String offerTitle;

    private String offerDescription;

    @NotBlank
    @Size(max = 255)
    private String requestTitle;

    @NotBlank
    private String requestDescription;

    private String agreedTerms;

    @NotNull
    @Min(1)
    private Integer estimatedHours;

    @NotNull
    @Min(0)
    private Integer agreedTokenAmount;

    @NotNull
    private Instant agreedDeadline;

    @NotBlank
    private String changeSummary;

    @Valid
    private List<AgreementVersionMilestoneRequestDto> milestones =
            new ArrayList<>();
}