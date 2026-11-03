import {
  User,
  Calendar,
  CheckCircle2,
  Clock,
} from "lucide-react";
import { useNavigate } from "react-router";

import type Session from "@/models/sessions/Session";
import SessionStatus from "@/models/sessions/SessionStatus";

import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

interface SessionCardProps {
  session: Session;
}

function SessionCard({ session }: SessionCardProps) {

  const navigate = useNavigate();

  const statusVariant = ():
    | "default"
    | "secondary"
    | "destructive"
    | "outline" => {

    switch (session.status) {

      case SessionStatus.CREATED:
        return "outline";

      case SessionStatus.IN_PROGRESS:
        return "secondary";

    case SessionStatus.PROVIDER_CONFIRMED:
        return "secondary";

    case SessionStatus.COMPLETED:
        return "default";

      case SessionStatus.CANCELLED:
        return "destructive";

      case SessionStatus.DISPUTED:
        return "destructive";

      default:
        return "outline";

    }

  };

  return (

<Card
  onClick={() => {

    console.log("CARD CLICKED");
    console.log(session.sessionId);

    navigate(`/dashboard/sessions/${session.sessionId}`);

  }}
  className="
    cursor-pointer
    transition-all
    hover:-translate-y-1
    hover:shadow-lg
  "
>

      <CardContent className="space-y-6 p-6">

        {/* Header */}

        <div className="flex items-start justify-between">

          <div>

            <h3 className="text-xl font-semibold">

              Service Session

            </h3>

            <p className="mt-1 text-sm text-muted-foreground">

              Agreement ID

            </p>

            <p className="font-mono text-xs">

              {session.agreementId}

            </p>

          </div>

          <Badge variant={statusVariant()}>

            {session.status}

          </Badge>

        </div>

        {/* Participants */}

        <div className="grid gap-4 md:grid-cols-2">

          <div className="flex items-center gap-3">

            <User size={18} />

            <div>

              <p className="text-xs text-muted-foreground">

                Requester

              </p>

              <p className="font-medium">

                {session.requesterName}

              </p>

            </div>

          </div>

          <div className="flex items-center gap-3">

            <User size={18} />

            <div>

              <p className="text-xs text-muted-foreground">

                Provider

              </p>

              <p className="font-medium">

                {session.providerName}

              </p>

            </div>

          </div>

        </div>

        {/* Progress */}

        <div className="rounded-lg border bg-muted/30 p-4">

          <h4 className="mb-4 font-medium">

            Session Progress

          </h4>

          <div className="space-y-3">

            <div className="flex items-center justify-between">

              <span>

                Started

              </span>

              {
                session.startedAt
                  ? (
                    <CheckCircle2
                      className="text-green-500"
                      size={18}
                    />
                  )
                  : (
                    <Clock
                      className="text-muted-foreground"
                      size={18}
                    />
                  )
              }

            </div>

            <div className="flex items-center justify-between">

              <span>

                Provider Confirmation

              </span>

              {
                session.providerConfirmed
                  ? (
                    <CheckCircle2
                      className="text-green-500"
                      size={18}
                    />
                  )
                  : (
                    <Clock
                      className="text-muted-foreground"
                      size={18}
                    />
                  )
              }

            </div>

            <div className="flex items-center justify-between">

              <span>

                Requester Confirmation

              </span>

              {
                session.requesterConfirmed
                  ? (
                    <CheckCircle2
                      className="text-green-500"
                      size={18}
                    />
                  )
                  : (
                    <Clock
                      className="text-muted-foreground"
                      size={18}
                    />
                  )
              }

            </div>

          </div>

        </div>

{/* Footer */}

<div className="flex items-center justify-between border-t pt-4">

    <div className="flex items-center gap-2 text-xs text-muted-foreground">

        <Calendar size={16} />

        <span>
            Created{" "}
            {session.createdAt
                ? new Date(session.createdAt).toLocaleDateString()
                : "-"}
        </span>

    </div>

    <Button
        variant="outline"
        onClick={(event) => {

            event.stopPropagation();

            navigate(
                `/dashboard/sessions/${session.sessionId}`
            );

        }}
    >
        View Session
    </Button>

</div>

      </CardContent>

    </Card>

  );

}

export default SessionCard;