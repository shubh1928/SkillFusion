package com.barterlink.marketplace.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateServiceRequestDto {

    @NotBlank
    private String title;

    @NotBlank
    private String description;

    @NotNull
    @Min(1)
    private Integer estimatedHours;

    @NotNull
    @Min(1)
    private Integer tokenCost;

    private Boolean selectiveRequest = false;
}