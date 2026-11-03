package com.barterlink.disputes.dto.response;

import com.barterlink.disputes.enums.DisputeEvidenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DisputeEvidenceItemDto {

    private UUID evidenceReferenceId;
    private DisputeEvidenceType evidenceType;
    private Instant attachedAt;

    // ID and descriptive information for the original record.
    private UUID sourceId;
    private String title;
    private String content;
    private String sourceStatus;
    private Instant sourceCreatedAt;

    // User who submitted the proof or authored the message/version.
    private String submittedByName;

    // Session-proof details.
    private String proofType;
    private String proofUrl;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private String sha256;

    // Negotiation-message details.
    private String attachmentUrl;
    private Boolean systemGenerated;

    // Agreement-version details.
    private Integer versionNumber;
}