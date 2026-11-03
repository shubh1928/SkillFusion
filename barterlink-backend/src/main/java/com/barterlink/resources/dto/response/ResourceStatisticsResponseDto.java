package com.barterlink.resources.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ResourceStatisticsResponseDto {

    private Long totalResources;

    private Long publicResources;

    private Long privateResources;

    private Long totalDownloads;

}