package com.barterlink.agreements.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class AgreementVersionMilestoneRequestDto {

    // Null means create a new milestone identity.
    // Existing IDs preserve the identity across versions.
    private UUID milestoneId;

    @NotNull
    @Min(1)
    private Integer milestoneNumber;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String deliverable;

    @NotNull
    private Instant deadline;

    @NotNull
    @Min(1)
    private Integer tokenAllocation;
}