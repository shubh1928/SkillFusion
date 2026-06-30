import type ProficiencyLevel from "../ProficiencyLevel";

export default interface UserSkill {

  id: string;

  skillId: string;

  skillName: string;

  category: string;

  proficiencyLevel: ProficiencyLevel;

}