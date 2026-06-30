import { useEffect, useMemo, useState } from "react";
import toast from "react-hot-toast";

import useAuth from "@/auth/store";

import {
  RefreshCw,
  Wrench,
} from "lucide-react";

import {
  getAllSkills,
  getUserSkills,
  addSkillToUser,
  removeUserSkill,
} from "@/services/SkillService";

import type Skill from "@/models/skills/Skill";
import type UserSkill from "@/models/skills/UserSkill";
import ProficiencyLevel from "@/models/ProficiencyLevel";

import SkillCard from "./SkillCard";
import UserSkillCard from "./UserSkillCard";

import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";

import {
  Card,
  CardContent,
} from "@/components/ui/card";

function SkillsPage() {

  const user =
    useAuth((state) => state.user);

  const [loading, setLoading] =
    useState(true);

  const [refreshing, setRefreshing] =
    useState(false);

  const [skills, setSkills] =
    useState<Skill[]>([]);

  const [userSkills, setUserSkills] =
    useState<UserSkill[]>([]);

      const loadSkills = async () => {

    if (!user?.id) {

      setLoading(false);

      return;

    }

    try {

      setLoading(true);

      const [

        availableSkills,

        currentUserSkills,

      ] = await Promise.all([

        getAllSkills(),

        getUserSkills(user.id),

      ]);

      setSkills(

        availableSkills

      );

      setUserSkills(

        currentUserSkills

      );

    } catch (error: any) {

      toast.error(

        error?.response?.data?.message ??

        "Failed to load skills"

      );

    } finally {

      setLoading(false);

    }

  };

useEffect(() => {

  if (user?.id) {

    loadSkills();

  }

}, [user?.id]);

  const handleRefresh = async () => {

    try {

      setRefreshing(true);

      await loadSkills();

      toast.success(

        "Skills refreshed"

      );

    } finally {

      setRefreshing(false);

    }

  };

  const handleAddSkill = async (

    skillId: string,

    proficiency: ProficiencyLevel

  ) => {

    if (!user?.id) {

      return;

    }

    try {

      await addSkillToUser(

        user.id,

        skillId,

        proficiency

      );

      toast.success(

        "Skill added successfully"

      );

      await loadSkills();

    } catch (error: any) {

      toast.error(

        error?.response?.data?.message ??

        "Unable to add skill"

      );

    }

  };

  const handleRemoveSkill = async (

    skillId: string

  ) => {

    if (!user?.id) {

      return;

    }

    try {

      await removeUserSkill(

        user.id,

        skillId

      );

      toast.success(

        "Skill removed successfully"

      );

      await loadSkills();

    } catch (error: any) {

      toast.error(

        error?.response?.data?.message ??

        "Unable to remove skill"

      );

    }

  };

  const availableSkills = useMemo(() => {

    const userSkillIds = new Set(

      userSkills.map(

        (skill) => skill.skillId

      )

    );

    return skills.filter(

      (skill) =>

        !userSkillIds.has(

          skill.id

        )

    );

  }, [

    skills,

    userSkills,

  ]);

    return (

    <div className="space-y-8">

      {/* Header */}

      <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">

        <div>

          <div className="flex items-center gap-3">

            <Wrench
              className="text-primary"
              size={30}
            />

            <h1 className="text-3xl font-bold">

              Skills

            </h1>

          </div>

          <p className="mt-2 text-muted-foreground">

            Manage your skills and showcase your expertise.

          </p>

        </div>

        <Button
          variant="outline"
          onClick={handleRefresh}
          disabled={refreshing}
        >

          {

            refreshing

              ? <Spinner />

              : <RefreshCw size={18} />

          }

          Refresh

        </Button>

      </div>

      {/* Loading */}

      {loading ? (

        <Card>

          <CardContent className="flex h-72 items-center justify-center">

            <div className="flex flex-col items-center gap-4">

              <Spinner className="h-10 w-10" />

              <p className="text-muted-foreground">

                Loading skills...

              </p>

            </div>

          </CardContent>

        </Card>

      ) : (

        <>
        
                  {/* My Skills */}

          <section className="space-y-6">

            <div className="flex items-center justify-between">

              <h2 className="text-2xl font-semibold">

                My Skills

              </h2>

              <span className="text-sm text-muted-foreground">

                {userSkills.length} Skill
                {userSkills.length !== 1 ? "s" : ""}

              </span>

            </div>

            {userSkills.length === 0 ? (

              <Card>

                <CardContent className="flex h-48 flex-col items-center justify-center text-center">

                  <Wrench
                    className="mb-4 text-muted-foreground"
                    size={56}
                  />

                  <h3 className="text-xl font-semibold">

                    No Skills Added

                  </h3>

                  <p className="mt-2 text-muted-foreground">

                    Add your first skill from the list below.

                  </p>

                </CardContent>

              </Card>

            ) : (

              <div className="grid gap-6 md:grid-cols-2">

                {userSkills.map((skill) => (

                  <UserSkillCard
                    key={skill.id}
                    skill={skill}
                    onRemove={handleRemoveSkill}
                  />

                ))}

              </div>

            )}

          </section>

          {/* Available Skills */}

          <section className="space-y-6">

            <div className="flex items-center justify-between">

              <h2 className="text-2xl font-semibold">

                Available Skills

              </h2>

              <span className="text-sm text-muted-foreground">

                {availableSkills.length} Skill
                {availableSkills.length !== 1 ? "s" : ""}

              </span>

            </div>

            {availableSkills.length === 0 ? (

              <Card>

                <CardContent className="flex h-48 flex-col items-center justify-center text-center">

                  <Wrench
                    className="mb-4 text-muted-foreground"
                    size={56}
                  />

                  <h3 className="text-xl font-semibold">

                    No More Skills

                  </h3>

                  <p className="mt-2 text-muted-foreground">

                    You have already added every available skill.

                  </p>

                </CardContent>

              </Card>

            ) : (

              <div className="grid gap-6 md:grid-cols-2">

                {availableSkills.map((skill) => (

                  <SkillCard
                    key={skill.id}
                    skill={skill}
                    onAdd={handleAddSkill}
                  />

                ))}

              </div>

            )}

          </section>

        </>

      )}

    </div>

  );

}

export default SkillsPage;