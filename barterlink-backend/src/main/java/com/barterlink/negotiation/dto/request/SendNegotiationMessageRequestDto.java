package com.barterlink.negotiation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SendNegotiationMessageRequestDto {

    @NotBlank(message = "Message cannot be empty")
    private String message;
}