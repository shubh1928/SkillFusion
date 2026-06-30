import apiClient from "@/config/ApiClient";

import type Skill from "@/models/skills/Skill";
import type UserSkill from "@/models/skills/UserSkill";
import type ProficiencyLevel from "@/models/ProficiencyLevel";

/**
 * Get all available skills
 */
export const getAllSkills =
  async (): Promise<Skill[]> => {

    const response = await apiClient.get(
      "/skills"
    );

    return response.data.data;

  };

/**
 * Get skills of a specific user
 */
export const getUserSkills =
  async (
    userId: string
  ): Promise<UserSkill[]> => {

    const response = await apiClient.get(
      `/skills/users/${userId}`
    );

    return response.data.data;

  };

/**
 * Add a skill to a user
 */
export const addSkillToUser =
  async (
    userId: string,
    skillId: string,
    proficiencyLevel: ProficiencyLevel
  ): Promise<void> => {

    await apiClient.post(
      `/skills/users/${userId}`,
      {
        skillId,
        proficiencyLevel,
      }
    );

  };

/**
 * Remove a user's skill
 */
export const removeUserSkill =
  async (
    userId: string,
    skillId: string
  ): Promise<void> => {

    await apiClient.delete(
      `/skills/users/${userId}/${skillId}`
    );

  };