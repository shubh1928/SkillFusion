import {
    CheckCircle2,
    Clock3,
} from "lucide-react";

import type Session from "@/models/sessions/Session";
import SessionWorkflow from "@/helpers/session/SessionWorkflow";
import InfoCard from "./InfoCard";

interface ProgressCardProps {
    session: Session | null;
    milestoneRelease?: boolean;
}

    const Step = ({
        label,
        completed,
    }: {
        label: string;
        completed: boolean;
    }) => (
        <div className="flex items-center justify-between">
            <span>{label}</span>

            {completed ? (
                <CheckCircle2
                    className="text-green-500"
                    size={18}
                />
            ) : (
                <Clock3
                    className="text-muted-foreground"
                    size={18}
                />
            )}
        </div>
    );


function ProgressCard({
    session,
    milestoneRelease = false,
}: ProgressCardProps) {

    const allProgressSteps = session
    ? SessionWorkflow.getProgressSteps(session)
    : [];

const progressSteps = milestoneRelease
    ? allProgressSteps.filter(
        (step) =>
            step.id === "provider-started" ||
            step.id === "requester-started",
      )
    : allProgressSteps;

    return (
        <InfoCard title="Session Progress">

            <div className="space-y-4">

                {progressSteps.map((step) => (

                    <Step
                        key={step.id}
                        label={step.label}
                        completed={step.completed}
                    />

                ))}

            </div>

        </InfoCard>
    );

}

export default ProgressCard;