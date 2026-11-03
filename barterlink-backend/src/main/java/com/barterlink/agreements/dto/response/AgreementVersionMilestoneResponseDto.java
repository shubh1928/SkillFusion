package com.barterlink.agreements.dto.response;

import com.barterlink.agreements.enums.AgreementMilestoneStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class AgreementVersionMilestoneResponseDto {

    private UUID versionMilestoneId;
    private UUID milestoneId;
    private Integer milestoneNumber;
    private String title;
    private String deliverable;
    private Instant deadline;
    private Integer tokenAllocation;
    private AgreementMilestoneStatus executionStatus;
}