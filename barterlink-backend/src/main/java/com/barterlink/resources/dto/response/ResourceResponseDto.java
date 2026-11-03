package com.barterlink.resources.dto.response;

import com.barterlink.resources.enums.ResourceCategory;
import com.barterlink.resources.enums.ResourceVisibility;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class ResourceResponseDto {

    private UUID resourceId;

    private UUID ownerId;

    private String ownerName;

    private String title;

    private String description;

    private ResourceCategory category;

    private ResourceVisibility visibility;

    private String originalFileName;

    private String contentType;

    private Long fileSize;

    private Integer downloadCount;

    private Instant createdAt;

}