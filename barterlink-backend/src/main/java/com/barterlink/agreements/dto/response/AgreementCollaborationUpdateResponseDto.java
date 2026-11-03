
package com.barterlink.agreements.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class AgreementCollaborationUpdateResponseDto {

    private String clientUpdateId;

    private UUID senderId;

    private String updatePayload;

    private Instant createdAt;
}
