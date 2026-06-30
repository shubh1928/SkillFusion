package com.barterlink.skills.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class SkillResponseDto {

    private UUID id;

    private String name;

    private String category;
}