package com.barterlink.sessions.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpenDisputeRequestDto {

    @NotBlank(message = "Dispute reason is required")
    @Size(
            max = 5000,
            message = "Dispute reason cannot exceed 5000 characters"
    )
    private String reason;
}