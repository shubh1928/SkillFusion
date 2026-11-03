
package com.barterlink.sessions.dto.response;

import com.barterlink.sessions.enums.ProofType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class SessionProofResponseDto {

    private UUID proofId;

    private UUID uploadedById;

    private String uploadedByName;

    private ProofType proofType;

    /**
     * External URL for link-based proofs.
     * Backend content endpoint for uploaded-file proofs.
     */
    private String proofUrl;

    /**
     * True when the proof contains an uploaded file.
     */
    private boolean fileAttachment;

    private String fileName;

    private String contentType;

    private Long fileSize;

    private String sha256;

    private String description;

    private Instant createdAt;
}
