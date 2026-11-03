package com.barterlink.resources.service;

import com.barterlink.resources.dto.request.UploadResourceRequestDto;
import com.barterlink.resources.dto.response.ResourceResponseDto;
import org.springframework.web.multipart.MultipartFile;
import com.barterlink.resources.dto.response.ResourceStatisticsResponseDto;
import com.barterlink.resources.dto.response.DownloadResourceResponseDto;

import java.util.List;
import java.util.UUID;

public interface ResourceService {

    ResourceResponseDto uploadResource(
            UUID ownerId,
            UploadResourceRequestDto dto,
            MultipartFile file
    );

    List<ResourceResponseDto> getMyResources(
            UUID ownerId
    );

    List<ResourceResponseDto> getPublicResources(
    );

    ResourceResponseDto getResourceById(
            UUID resourceId
    );

    DownloadResourceResponseDto downloadResource(
            UUID resourceId
    );

    void deleteResource(
            UUID ownerId,
            UUID resourceId
    );

    List<ResourceResponseDto> getResourcesByUser(
            UUID userId
    );

    ResourceStatisticsResponseDto getMyStatistics(
            UUID ownerId
    );


}