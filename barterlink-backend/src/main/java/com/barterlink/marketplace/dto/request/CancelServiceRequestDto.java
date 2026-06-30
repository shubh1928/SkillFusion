package com.barterlink.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelServiceRequestDto {

    @NotBlank
    private String reason;
}
