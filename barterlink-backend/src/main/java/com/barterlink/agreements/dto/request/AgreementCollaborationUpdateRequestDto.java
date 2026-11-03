
package com.barterlink.agreements.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AgreementCollaborationUpdateRequestDto {

    @NotBlank
    @Size(max = 36)
    private String clientUpdateId;

    @NotBlank
    @Size(max = 1_398_104)
    private String updatePayload;
}
