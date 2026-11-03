package com.barterlink.resources.service.impl;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.resources.dto.request.UploadResourceRequestDto;
import com.barterlink.resources.dto.response.ResourceResponseDto;
import com.barterlink.resources.dto.response.ResourceStatisticsResponseDto;
import com.barterlink.resources.entity.Resource;
import com.barterlink.resources.enums.ResourceVisibility;
import com.barterlink.resources.mapper.ResourceMapper;
import com.barterlink.resources.repository.ResourceRepository;
import com.barterlink.resources.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.barterlink.resources.ipfs.dto.PinataUploadResponse;
import com.barterlink.resources.ipfs.service.PinataService;
import com.barterlink.common.exception.ForbiddenException;
import com.barterlink.resources.dto.response.DownloadResourceResponseDto;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResourceServiceImpl
        implements ResourceService {

    private final ResourceRepository resourceRepository;

    private final UserRepository userRepository;

    private final ResourceMapper resourceMapper;

    private final PinataService pinataService;


    @Override
    public List<ResourceResponseDto> getMyResources(
            UUID ownerId
    ) {

        User owner =
                userRepository.findById(ownerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        return resourceRepository

                .findByOwnerOrderByCreatedAtDesc(owner)

                .stream()

                .map(resourceMapper::toResponse)

                .toList();

    }

    @Override
    public List<ResourceResponseDto> getPublicResources() {

        return resourceRepository

                .findByVisibilityOrderByCreatedAtDesc(
                        ResourceVisibility.PUBLIC
                )

                .stream()

                .map(resourceMapper::toResponse)

                .toList();

    }

    @Override
    public ResourceResponseDto getResourceById(
            UUID resourceId
    ) {

        Resource resource =

                resourceRepository.findById(resourceId)

                        .orElseThrow(() ->

                                new ResourceNotFoundException(
                                        "Resource not found"
                                ));

        return resourceMapper.toResponse(resource);

    }

    @Override
    public DownloadResourceResponseDto downloadResource(
            UUID resourceId
    ) {

        Resource resource =

                resourceRepository.findById(resourceId)

                        .orElseThrow(() ->

                                new ResourceNotFoundException(
                                        "Resource not found"
                                ));

        resource.setDownloadCount(

                resource.getDownloadCount() + 1

        );

        resourceRepository.save(resource);

        return new DownloadResourceResponseDto(

                "https://gateway.pinata.cloud/ipfs/"

                        + resource.getCid()

        );

    }
    @Override
    public ResourceResponseDto uploadResource(
            UUID ownerId,
            UploadResourceRequestDto dto,
            MultipartFile file
    ) {

        User owner =
                userRepository.findById(ownerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        try {

            PinataUploadResponse uploadResponse =
                    pinataService.uploadFile(file);

            Resource resource =
                    Resource.builder()

                            .owner(owner)

                            .title(dto.getTitle())

                            .description(dto.getDescription())

                            .category(dto.getCategory())

                            .visibility(dto.getVisibility())

                            .originalFileName(
                                    uploadResponse.getFileName()
                            )

                            .contentType(
                                    file.getContentType()
                            )

                            .fileSize(
                                    uploadResponse.getFileSize()
                            )

                            .cid(
                                    uploadResponse.getCid()
                            )

                            .pinataFileId(

                                    uploadResponse.getId()

                            )

                            .build();

            Resource savedResource =
                    resourceRepository.save(resource);

            return resourceMapper.toResponse(savedResource);

        }
        catch (Exception ex) {

            System.out.println("========== RESOURCE UPLOAD ERROR ==========");
            ex.printStackTrace();
            System.out.println("===========================================");

            throw new RuntimeException(
                    "Failed to upload resource",
                    ex
            );

        }

    }

    @Override
    public void deleteResource(
            UUID ownerId,
            UUID resourceId
    ) {

        Resource resource =

                resourceRepository.findById(resourceId)

                        .orElseThrow(() ->

                                new ResourceNotFoundException(
                                        "Resource not found"
                                ));

        if (!resource.getOwner().getId().equals(ownerId)) {

            throw new ForbiddenException(
                    "You can delete only your own resources"
            );

        }

        try {

            pinataService.deleteFile(

                    resource.getPinataFileId()

            );

        }
        catch (Exception ex) {

            throw new RuntimeException(
                    "Failed to delete resource from IPFS",
                    ex
            );

        }

        resourceRepository.delete(resource);

    }


    @Override
    public List<ResourceResponseDto> getResourcesByUser(
            UUID userId
    ) {

        User owner =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        return resourceRepository

                .findByOwnerAndVisibilityOrderByCreatedAtDesc(

                        owner,

                        ResourceVisibility.PUBLIC

                )

                .stream()

                .map(resourceMapper::toResponse)

                .toList();

    }


    @Override
    public ResourceStatisticsResponseDto getMyStatistics(
            UUID ownerId
    ) {

        User owner =

                userRepository.findById(ownerId)

                        .orElseThrow(() ->

                                new ResourceNotFoundException(
                                        "User not found"
                                ));

        long totalResources =

                resourceRepository.countByOwner(owner);

        long publicResources =

                resourceRepository.countByOwnerAndVisibility(

                        owner,

                        ResourceVisibility.PUBLIC

                );

        long privateResources =

                resourceRepository.countByOwnerAndVisibility(

                        owner,

                        ResourceVisibility.PRIVATE

                );

        long totalDownloads =

                resourceRepository.getTotalDownloadsByOwner(

                        owner

                );

        return ResourceStatisticsResponseDto.builder()

                .totalResources(totalResources)

                .publicResources(publicResources)

                .privateResources(privateResources)

                .totalDownloads(totalDownloads)

                .build();

    }

}