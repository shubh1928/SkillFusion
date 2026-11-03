import type Session from "@/models/sessions/Session";

import InfoCard from "./InfoCard";

interface ParticipantsCardProps {
    session: Session | null;
}

function ParticipantsCard({
    session,
}: ParticipantsCardProps) {

    return (

        <InfoCard title="Participants">

            <div className="space-y-4">

                <div>

                    <p className="text-xs text-muted-foreground">
                        Requester
                    </p>

                    <p className="font-medium">
                        {session?.requesterName}
                    </p>

                </div>

                <div>

                    <p className="text-xs text-muted-foreground">
                        Provider
                    </p>

                    <p className="font-medium">
                        {session?.providerName}
                    </p>

                </div>

            </div>

        </InfoCard>

    );

}

export default ParticipantsCard;