package com.barterlink.resources.dto.request;

import com.barterlink.resources.enums.ResourceCategory;
import com.barterlink.resources.enums.ResourceVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UploadResourceRequestDto {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private ResourceCategory category;

    @NotNull
    private ResourceVisibility visibility;

}