package com.barterlink.skills.controller;

import com.barterlink.common.response.ApiResponse;
import com.barterlink.skills.dto.SkillRequestDto;
import com.barterlink.skills.dto.SkillResponseDto;
import com.barterlink.skills.dto.request.UserSkillRequestDto;
import com.barterlink.skills.dto.response.UserSkillResponseDto;
import com.barterlink.skills.service.SkillService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @PostMapping
    public ApiResponse<SkillResponseDto> createSkill(
            @Valid @RequestBody SkillRequestDto request) {

        return ApiResponse.success(
                "Skill created successfully",
                skillService.createSkill(request)
        );
    }

    @GetMapping
    public ApiResponse<List<SkillResponseDto>> getAllSkills() {

        return ApiResponse.success(
                skillService.getAllSkills()
        );
    }

    @GetMapping("/{skillId}")
    public ApiResponse<SkillResponseDto> getSkillById(
            @PathVariable UUID skillId) {

        return ApiResponse.success(
                skillService.getSkillById(skillId)
        );
    }

    @PostMapping("/users/{userId}")
    public ApiResponse<Void> addSkillToUser(
            @PathVariable UUID userId,
            @Valid @RequestBody UserSkillRequestDto request) {

        skillService.addSkillToUser(userId, request);

        return ApiResponse.success(
                "Skill added to user successfully"
        );
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<List<UserSkillResponseDto>> getUserSkills(
            @PathVariable UUID userId) {

        return ApiResponse.success(
                skillService.getUserSkills(userId)
        );
    }

    @DeleteMapping("/users/{userId}/{skillId}")
    public ApiResponse<Void> removeUserSkill(
            @PathVariable UUID userId,
            @PathVariable UUID skillId) {

        skillService.removeUserSkill(userId, skillId);

        return ApiResponse.success(
                "Skill removed successfully"
        );
    }
}