package com.barterlink.skills.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SkillRequestDto {

    @NotBlank(message = "Skill name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Category is required")
    @Size(max = 100)
    private String category;
}