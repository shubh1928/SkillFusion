import type Session from "@/models/sessions/Session";
import SessionWorkflow, {type SessionAction} from "@/helpers/session/SessionWorkflow";

interface SessionActionsCardProps {
    session: Session | null;
    userId?: string;

    onStartSession: () => void;
    onProviderComplete: () => void;
    onRequesterComplete: () => void;
    onCancelSession: () => void;
    onDisputeSession: () => void;
    milestoneRelease?: boolean;
}

function SessionActionsCard({
    session,
    userId,
    onStartSession,
    onProviderComplete,
    onRequesterComplete,
    onCancelSession,
    onDisputeSession,
    milestoneRelease = false,
}: SessionActionsCardProps) {

    if (!session) {
        return null;
    }

    const availableActions = SessionWorkflow.getAvailableActions(
    session,
    userId ?? "",
    );

const actions = milestoneRelease
    ? availableActions.filter(
        (action) => action.id === "START",
      )
    : availableActions;

    const handleAction = (handler: SessionAction["handler"]) => {

    switch (handler) {

        case "start":
            onStartSession();
            break;

        case "complete":
            onProviderComplete();
            break;

        case "confirm":
            onRequesterComplete();
            break;

        case "cancel":
            onCancelSession();
            break;

        case "dispute":
            onDisputeSession();
            break;

    }

};

    return (

        <div className="rounded-xl border p-4">

            <h3 className="mb-4 font-semibold">
                Session Actions
            </h3>

            <div className="space-y-3">

    {actions.map((action) => {

        let className = "w-full rounded-lg px-4 py-2";

        switch (action.variant) {

            case "default":
                className +=
                    " bg-primary text-primary-foreground";
                break;

            case "secondary":
                className +=
                    " border";
                break;

            case "destructive":
                className +=
                    " border border-red-500 text-red-600";
                break;

        }

        return (

            <button
                key={action.id}
                onClick={() => handleAction(action.handler)}
                className={className}
            >
                {action.label}
            </button>

        );

    })}

</div>

        </div>

    );

}

export default SessionActionsCard;