package com.barterlink.sessions.dto.request;

import com.barterlink.sessions.enums.ProofType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadProofRequestDto {

    @NotNull(message = "Proof type is required")
    private ProofType proofType;

    private String proofUrl;

    private String description;
}
