package com.barterlink.skills.service;

import com.barterlink.skills.dto.SkillRequestDto;
import com.barterlink.skills.dto.SkillResponseDto;
import com.barterlink.skills.dto.request.UserSkillRequestDto;
import com.barterlink.skills.dto.response.UserSkillResponseDto;

import java.util.List;
import java.util.UUID;

public interface SkillService {

    SkillResponseDto createSkill(SkillRequestDto request);

    List<SkillResponseDto> getAllSkills();

    SkillResponseDto getSkillById(UUID skillId);

    void addSkillToUser(UUID userId, UserSkillRequestDto request);

    List<UserSkillResponseDto> getUserSkills(UUID userId);

    void removeUserSkill(UUID userId, UUID skillId);
}