package com.barterlink.skills.dto.request;

import com.barterlink.skills.enums.ProficiencyLevel;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class UserSkillRequestDto {

    @NotNull(message = "Skill id is required")
    private UUID skillId;

    @NotNull(message = "Proficiency level is required")
    private ProficiencyLevel proficiencyLevel;
}