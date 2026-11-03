package com.barterlink.disputes.dto.request;

import com.barterlink.disputes.enums.DisputeDecisionOutcome;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CastDisputeVoteRequestDto {

    @NotNull(message = "A decision outcome is required")
    private DisputeDecisionOutcome outcome;

    @NotBlank(message = "A reason for the vote is required")
    @Size(
            max = 5000,
            message = "The voting reason cannot exceed 5000 characters"
    )
    private String reason;
}