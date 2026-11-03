package com.barterlink.resources.controller;

import com.barterlink.auth.security.CurrentUserUtil;
import com.barterlink.common.response.ApiResponse;
import com.barterlink.resources.dto.request.UploadResourceRequestDto;
import com.barterlink.resources.dto.response.ResourceResponseDto;
import com.barterlink.resources.service.ResourceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.barterlink.resources.dto.response.ResourceStatisticsResponseDto;
import com.barterlink.resources.dto.response.DownloadResourceResponseDto;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    private final CurrentUserUtil currentUserUtil;

    @PostMapping(
            consumes = "multipart/form-data"
    )
    public ApiResponse<ResourceResponseDto> uploadResource(

            @RequestPart("data")
            @Valid
            UploadResourceRequestDto dto,

            @RequestPart("file")
            MultipartFile file
    ) {

        return ApiResponse.success(

                "Resource uploaded successfully",

                resourceService.uploadResource(

                        currentUserUtil.getCurrentUserId(),

                        dto,

                        file

                )

        );

    }

    @GetMapping("/my")
    public ApiResponse<List<ResourceResponseDto>>
    getMyResources() {

        return ApiResponse.success(

                resourceService.getMyResources(

                        currentUserUtil.getCurrentUserId()

                )

        );

    }


    @GetMapping("/my/statistics")
    public ApiResponse<ResourceStatisticsResponseDto>
    getMyStatistics() {

        return ApiResponse.success(

                resourceService.getMyStatistics(

                        currentUserUtil.getCurrentUserId()

                )

        );

    }


    @GetMapping("/public")
    public ApiResponse<List<ResourceResponseDto>>
    getPublicResources() {

        return ApiResponse.success(

                resourceService.getPublicResources()

        );

    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<ResourceResponseDto>>
    getResourcesByUser(

            @PathVariable
            UUID userId
    ) {

        return ApiResponse.success(

                resourceService.getResourcesByUser(
                        userId
                )

        );

    }

    @GetMapping("/{resourceId}/download")
    public ApiResponse<DownloadResourceResponseDto>
    downloadResource(

            @PathVariable
            UUID resourceId
    ) {

        return ApiResponse.success(

                resourceService.downloadResource(
                        resourceId
                )

        );

    }


    @GetMapping("/{resourceId}")
    public ApiResponse<ResourceResponseDto>
    getResourceById(

            @PathVariable
            UUID resourceId
    ) {

        return ApiResponse.success(

                resourceService.getResourceById(resourceId)

        );

    }

    @DeleteMapping("/{resourceId}")
    public ApiResponse<Void> deleteResource(

            @PathVariable
            UUID resourceId
    ) {

        resourceService.deleteResource(

                currentUserUtil.getCurrentUserId(),

                resourceId

        );

        return ApiResponse.success(

                "Resource deleted successfully"

        );

    }

}