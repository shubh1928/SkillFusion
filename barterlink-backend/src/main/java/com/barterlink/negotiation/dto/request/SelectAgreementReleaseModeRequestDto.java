
package com.barterlink.negotiation.dto.request;

import com.barterlink.agreements.enums.AgreementReleaseMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SelectAgreementReleaseModeRequestDto {

    @NotNull(message = "Release mode is required")
    private AgreementReleaseMode releaseMode;

    @Min(value = 2, message = "At least 2 milestones are required")
    @Max(value = 5, message = "A maximum of 5 milestones is allowed")
    private Integer milestoneCount;
}
