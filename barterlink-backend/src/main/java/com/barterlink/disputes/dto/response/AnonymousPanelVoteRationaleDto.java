package com.barterlink.disputes.dto.response;

import com.barterlink.disputes.enums.DisputeDecisionOutcome;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnonymousPanelVoteRationaleDto {

    private DisputeDecisionOutcome outcome;

    private String reason;
}