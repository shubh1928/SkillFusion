package com.barterlink.skills.dto.response;

import com.barterlink.skills.enums.ProficiencyLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UserSkillResponseDto {

    private UUID id;

    private UUID skillId;

    private String skillName;

    private String category;

    private ProficiencyLevel proficiencyLevel;
}