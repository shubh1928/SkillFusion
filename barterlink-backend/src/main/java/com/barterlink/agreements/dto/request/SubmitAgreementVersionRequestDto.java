
package com.barterlink.agreements.dto.request;

import jakarta.validation.Valid;
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
public class SubmitAgreementVersionRequestDto {

    @Size(max = 10000)
    private String agreedTerms;

    @NotNull
    private Instant agreedDeadline;

    @NotBlank
    @Size(max = 255)
    private String changeSummary;

    @Valid
    private List<AgreementVersionMilestoneRequestDto> milestones =
            new ArrayList<>();
}
