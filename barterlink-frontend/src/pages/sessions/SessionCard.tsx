import {
  User,
  Calendar,
  CheckCircle2,
  Clock,
} from "lucide-react";

import type Session from "@/models/sessions/Session";
import SessionStatus from "@/models/sessions/SessionStatus";

import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

interface SessionCardProps {

  session: Session;

  currentUserId: string;

  onStart?: (sessionId: string) => void;

  onProviderConfirm?: (
    sessionId: string
  ) => void;

  onRequesterConfirm?: (
    sessionId: string
  ) => void;

  onCancel?: (
    sessionId: string
  ) => void;

  onDispute?: (
    sessionId: string
  ) => void;

}

function SessionCard({

  session,

  currentUserId,

  onStart,

  onProviderConfirm,

  onRequesterConfirm,

  onCancel,

  onDispute,

}: SessionCardProps) {

  const isRequester =
    session.requesterId === currentUserId;

  const isProvider =
    session.providerId === currentUserId;

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

    <Card className="transition-all hover:-translate-y-1 hover:shadow-lg">

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

          {/* Action Buttons */}

          <div className="flex flex-wrap gap-2">

            {/* CREATED */}

{session.status === SessionStatus.CREATED && (

  <>

    {isProvider && (

      <Button
        onClick={() =>
          onStart?.(session.sessionId)
        }
      >
        Start Session
      </Button>

    )}

    {!isProvider && isRequester && (

      <Badge variant="outline">

        Waiting for Provider

      </Badge>

    )}

    <Button
      variant="outline"
      onClick={() =>
        onDispute?.(session.sessionId)
      }
    >
      Dispute
    </Button>

    <Button
      variant="destructive"
      onClick={() =>
        onCancel?.(session.sessionId)
      }
    >
      Cancel
    </Button>

  </>

)}

{/* IN PROGRESS */}

{session.status === SessionStatus.IN_PROGRESS && (

  <>

    {isProvider && (

      <Button
        onClick={() =>
          onProviderConfirm?.(
            session.sessionId
          )
        }
      >
        Confirm Completion
      </Button>

    )}

    {!isProvider && isRequester && (

      <Badge variant="outline">

        Waiting for Provider Confirmation

      </Badge>

    )}

    <Button
      variant="outline"
      onClick={() =>
        onDispute?.(session.sessionId)
      }
    >
      Dispute
    </Button>

    <Button
      variant="destructive"
      onClick={() =>
        onCancel?.(session.sessionId)
      }
    >
      Cancel
    </Button>

  </>

)}

{/* PROVIDER CONFIRMED */}

{session.status ===
  SessionStatus.PROVIDER_CONFIRMED && (

  <>

    {isRequester && (

      <Button
        onClick={() =>
          onRequesterConfirm?.(
            session.sessionId
          )
        }
      >
        Final Confirm
      </Button>

    )}

    {!isRequester && isProvider && (

      <Badge variant="outline">

        Waiting for Requester

      </Badge>

    )}

    <Button
      variant="outline"
      onClick={() =>
        onDispute?.(session.sessionId)
      }
    >
      Dispute
    </Button>

  </>

)}

{/* COMPLETED */}

{session.status ===
  SessionStatus.COMPLETED && (

  <Badge>

    Session Completed

  </Badge>

)}

{/* CANCELLED */}

{session.status ===
  SessionStatus.CANCELLED && (

  <Badge variant="destructive">

    Session Cancelled

  </Badge>

)}

{/* DISPUTED */}

{session.status ===
  SessionStatus.DISPUTED && (

  <Badge variant="destructive">

    Session Disputed

  </Badge>

)}

          </div>

        </div>

      </CardContent>

    </Card>

  );

}

export default SessionCard;