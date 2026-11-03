package com.barterlink.skills.service.impl;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.ConflictException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.skills.dto.SkillRequestDto;
import com.barterlink.skills.dto.SkillResponseDto;
import com.barterlink.skills.dto.request.UserSkillRequestDto;
import com.barterlink.skills.dto.response.UserSkillResponseDto;
import com.barterlink.skills.entity.Skill;
import com.barterlink.skills.entity.UserSkill;
import com.barterlink.skills.repository.SkillRepository;
import com.barterlink.skills.repository.UserSkillRepository;
import com.barterlink.skills.service.SkillService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillRepository skillRepository;

    private final UserSkillRepository userSkillRepository;

    private final UserRepository userRepository;

    private final ModelMapper modelMapper;

    @Override
    public SkillResponseDto createSkill(SkillRequestDto request) {

        if (skillRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ConflictException("Skill already exists");
        }

        Skill skill = Skill.builder()
                .name(request.getName())
                .category(request.getCategory())
                .build();

        Skill savedSkill = skillRepository.save(skill);

        return modelMapper.map(savedSkill, SkillResponseDto.class);
    }

    @Override
    public List<SkillResponseDto> getAllSkills() {

        return skillRepository.findAll()
                .stream()
                .map(skill -> modelMapper.map(skill, SkillResponseDto.class))
                .toList();
    }

    @Override
    public SkillResponseDto getSkillById(UUID skillId) {

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Skill not found"));

        return modelMapper.map(skill, SkillResponseDto.class);
    }

    @Override
    public void addSkillToUser(
            UUID userId,
            UserSkillRequestDto request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (isUnavailableForMarketplace(user)) {
            throw new ConflictException(
                    "Cannot add skills to a disabled account."
            );
        }

        Skill skill = skillRepository.findById(request.getSkillId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Skill not found"));

        if (userSkillRepository.existsByUserAndSkill(user, skill)) {
            throw new ConflictException("Skill already added");
        }

        UserSkill userSkill = UserSkill.builder()
                .user(user)
                .skill(skill)
                .proficiencyLevel(request.getProficiencyLevel())
                .build();

        userSkillRepository.save(userSkill);
    }

    @Override
    public List<UserSkillResponseDto> getUserSkills(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (isUnavailableForMarketplace(user)) {
            throw new ResourceNotFoundException("User not found");
        }

        return userSkillRepository.findByUser(user)
                .stream()
                .map(userSkill -> {

                    UserSkillResponseDto dto =
                            new UserSkillResponseDto();

                    dto.setId(userSkill.getId());

                    dto.setSkillId(
                            userSkill.getSkill().getId());

                    dto.setSkillName(
                            userSkill.getSkill().getName());

                    dto.setCategory(
                            userSkill.getSkill().getCategory());

                    dto.setProficiencyLevel(
                            userSkill.getProficiencyLevel());

                    return dto;

                }).toList();
    }

    @Override
    public void removeUserSkill(
            UUID userId,
            UUID skillId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));

        if (isUnavailableForMarketplace(user)) {
            throw new ConflictException(
                    "Cannot modify skills for a disabled account."
            );
        }

        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Skill not found"));

        UserSkill userSkill =
                userSkillRepository.findByUserAndSkill(user, skill)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User skill not found"));

        userSkillRepository.delete(userSkill);
    }

    private boolean isUnavailableForMarketplace(User user) {
        return !user.isEnabled()
                || user.getInactivityDeactivatedAt() != null
                || user.getAnonymizedAt() != null;
    }

}