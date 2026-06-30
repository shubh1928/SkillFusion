import {
  BadgeCheck,
  Plus,
  Tag,
} from "lucide-react";

import type Skill from "@/models/skills/Skill";
import ProficiencyLevel from "@/models/ProficiencyLevel";

import {
  Card,
  CardContent,
} from "@/components/ui/card";

import { Badge } from "@/components/ui/badge";

import { Button } from "@/components/ui/button";

interface SkillCardProps {

  skill: Skill;

  onAdd: (
    skillId: string,
    proficiency: ProficiencyLevel
  ) => void;

}

function SkillCard({

  skill,

  onAdd,

}: SkillCardProps) {

  const badgeVariant = ():
    | "default"
    | "secondary"
    | "outline"
    | "destructive" => {

    switch (skill.category.toLowerCase()) {

      case "programming":
        return "default";

      case "database":
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

          <div className="space-y-1">

            <div className="flex items-center gap-2">

              <BadgeCheck
                className="text-primary"
                size={20}
              />

              <h3 className="text-lg font-semibold">

                {skill.name}

              </h3>

            </div>

            <div className="flex items-center gap-2">

              <Tag
                size={15}
                className="text-muted-foreground"
              />

              <Badge
                variant={badgeVariant()}
              >

                {skill.category}

              </Badge>

            </div>

          </div>

        </div>

        {/* Footer */}

        <div className="flex justify-end border-t pt-4">

          <Button
            onClick={() =>
            onAdd(
                skill.id,
                ProficiencyLevel.BEGINNER
            )
            }
          >

            <Plus size={16} />

            Add Skill

          </Button>

        </div>

      </CardContent>

    </Card>

  );

}

export default SkillCard;