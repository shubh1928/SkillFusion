import {
  Award,
  Trash2,
  Tag,
} from "lucide-react";

import type UserSkill from "@/models/skills/UserSkill";
import ProficiencyLevel from "@/models/ProficiencyLevel";

import {
  Card,
  CardContent,
} from "@/components/ui/card";

import { Badge } from "@/components/ui/badge";

import { Button } from "@/components/ui/button";

interface UserSkillCardProps {

  skill: UserSkill;

  onRemove: (
    skillId: string
  ) => void;

}

function UserSkillCard({

  skill,

  onRemove,

}: UserSkillCardProps) {

  const proficiencyVariant = ():
    | "default"
    | "secondary"
    | "outline"
    | "destructive" => {

    switch (skill.proficiencyLevel) {

      case ProficiencyLevel.EXPERT:
        return "default";

      case ProficiencyLevel.INTERMEDIATE:
        return "secondary";

      default:
        return "outline";

    }

  };

    return (

    <Card className="transition-all hover:-translate-y-1 hover:shadow-lg">

      <CardContent className="space-y-5 p-6">

        {/* Header */}

        <div className="flex items-start justify-between">

          <div className="space-y-2">

            <div className="flex items-center gap-2">

              <Award
                className="text-primary"
                size={20}
              />

              <h3 className="text-lg font-semibold">

                {skill.skillName}

              </h3>

            </div>

            <div className="flex items-center gap-2">

              <Tag
                size={15}
                className="text-muted-foreground"
              />

              <Badge variant="outline">

                {skill.category}

              </Badge>

            </div>

          </div>

          <Badge variant={proficiencyVariant()}>

            {skill.proficiencyLevel}

          </Badge>

        </div>

        {/* Footer */}

        <div className="flex justify-end border-t pt-4">

          <Button
            variant="destructive"
            onClick={() =>
              onRemove(
                skill.skillId
              )
            }
          >

            <Trash2 size={16} />

            Remove

          </Button>

        </div>

      </CardContent>

    </Card>

  );

}

export default UserSkillCard;