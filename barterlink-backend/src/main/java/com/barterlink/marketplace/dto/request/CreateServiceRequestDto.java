package com.barterlink.marketplace.dto.request;

import com.barterlink.marketplace.enums.ExchangeMode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateServiceRequestDto {

    @NotBlank
    private String title;

    @NotNull
    @Min(1)
    private Integer estimatedHours;

    @NotNull
    private ExchangeMode exchangeMode;

    private String offerTitle;

    private String offerDescription;

    @NotBlank
    private String requestTitle;

    private List<UUID> skillIds;

    @NotBlank
    private String requestDescription;

    private Integer tokenAmount;

    private Boolean selectiveRequest = false;
}