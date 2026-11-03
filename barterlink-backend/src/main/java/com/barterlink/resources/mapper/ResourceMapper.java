package com.barterlink.resources.mapper;

import com.barterlink.resources.dto.response.ResourceResponseDto;
import com.barterlink.resources.entity.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResourceMapper {

    public ResourceResponseDto toResponse(
            Resource resource
    ) {

        ResourceResponseDto dto =
                new ResourceResponseDto();

        dto.setResourceId(
                resource.getId()
        );

        dto.setOwnerId(
                resource.getOwner().getId()
        );

        dto.setOwnerName(
                resource.getOwner().getName()
        );

        dto.setTitle(
                resource.getTitle()
        );

        dto.setDescription(
                resource.getDescription()
        );

        dto.setCategory(
                resource.getCategory()
        );

        dto.setVisibility(
                resource.getVisibility()
        );

        dto.setOriginalFileName(
                resource.getOriginalFileName()
        );

        dto.setContentType(
                resource.getContentType()
        );

        dto.setFileSize(
                resource.getFileSize()
        );

        dto.setDownloadCount(
                resource.getDownloadCount()
        );

        dto.setCreatedAt(
                resource.getCreatedAt()
        );

        return dto;
    }

}