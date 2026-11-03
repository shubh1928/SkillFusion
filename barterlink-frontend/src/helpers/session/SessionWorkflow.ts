import ExchangeMode from "@/models/sessions/ExchangeMode";
import type Session from "@/models/sessions/Session";

export interface SessionAction {

    id:
        | "START"
        | "MARK_COMPLETED"
        | "CONFIRM_COMPLETION"
        | "DISPUTE"
        | "CANCEL";

    label: string;

    primary?: boolean;

    variant?: "default" | "destructive" | "secondary";

    handler:
    | "start"
    | "complete"
    | "confirm"
    | "cancel"
    | "dispute";

}

export interface ProgressStep {

    id: string;

    label: string;

    completed: boolean;

}

export default class SessionWorkflow {

static isCompleted(session: Session): boolean {

    return session.status === "COMPLETED";

}


static getProgressSteps(
    session: Session
): ProgressStep[] {

    switch (session.exchangeMode) {

        case ExchangeMode.TOKEN_BASED:

            return [

                {
                    id: "provider-started",
                    label: "Provider Started",
                    completed: !!session.providerStarted
                },

                {
                    id: "requester-started",
                    label: "Requester Started",
                    completed: !!session.requesterStarted
                },

                {
                    id: "provider-completed",
                    label: "Provider Completed",
                    completed: !!session.providerCompletedAt
                },

                {
                    id: "requester-confirmed",
                    label: "Requester Confirmed",
                    completed: !!session.requesterConfirmed
                }

            ];

        case ExchangeMode.DIRECT_BARTER:
    return [

        {
            id: "provider-started",
            label: "Provider Started",
            completed: !!session.providerStarted
        },

        {
            id: "requester-started",
            label: "Requester Started",
            completed: !!session.requesterStarted
        },

        {
            id: "provider-completed",
            label: "Provider Completed",
            completed: !!session.providerCompletedAt
        },

        {
            id: "requester-completed",
            label: "Requester Completed",
            completed: !!session.requesterCompletedAt
        },

        {
            id: "provider-confirmed",
            label: "Provider Confirmed",
            completed: !!session.providerConfirmed
        },

        {
            id: "requester-confirmed",
            label: "Requester Confirmed",
            completed: !!session.requesterConfirmed
        }

    ];

case ExchangeMode.HYBRID:
    return [

        {
            id: "provider-started",
            label: "Provider Started",
            completed: !!session.providerStarted
        },

        {
            id: "requester-started",
            label: "Requester Started",
            completed: !!session.requesterStarted
        },

        {
            id: "provider-completed",
            label: "Provider Completed",
            completed: !!session.providerCompletedAt
        },

        {
            id: "requester-completed",
            label: "Requester Completed",
            completed: !!session.requesterCompletedAt
        },

        {
            id: "provider-confirmed",
            label: "Provider Confirmed",
            completed: !!session.providerConfirmed
        },

        {
            id: "requester-confirmed",
            label: "Requester Confirmed",
            completed: !!session.requesterConfirmed
        }

    ];

        default:

            return [];

    }

}

static getAvailableActions(
    session: Session,
    currentUserId: string
): SessionAction[] {

    const actions: SessionAction[] = [];

const isProvider =
    currentUserId === session.providerId;

const isRequester =
    currentUserId === session.requesterId;

    switch (session.exchangeMode) {

    case ExchangeMode.TOKEN_BASED:

    // ----------------------------
    // START SESSION
    // ----------------------------

    if (
        session.status === "CREATED"
    ) {

        if (
            isProvider &&
            !session.providerStarted
        ) {

            actions.push({

                id: "START",

                label: "Start Session",

                handler: "start",

                primary: true,

                variant: "default",

            });

        }

        if (
            isRequester &&
            !session.requesterStarted
        ) {

            actions.push({

                id: "START",

                label: "Start Session",

                handler: "start",

                primary: true,

                variant: "default",

            });

        }

    }

    // ----------------------------
    // MARK WORK COMPLETED
    // ----------------------------

    if (
        session.status === "IN_PROGRESS" &&
        isProvider &&
        !session.providerCompletedAt
    ) {

        actions.push({

            id: "MARK_COMPLETED",

            label: "Mark Work Completed",

            handler: "complete",

            primary: true,

            variant: "default",

        });

    }

    // ----------------------------
    // CONFIRM COMPLETION
    // ----------------------------

    if (
        session.status === "IN_PROGRESS" &&
        isRequester &&
        !!session.providerCompletedAt &&
        !session.requesterConfirmed
    ) {

        actions.push({

            id: "CONFIRM_COMPLETION",

            label: "Confirm Completion",

            handler: "confirm",

            primary: true,

            variant: "default",

        });

    }

    // ----------------------------
    // COMMON ACTIONS
    // ----------------------------

    if (
        session.status === "CREATED" ||
        session.status === "IN_PROGRESS"
    ) {

        actions.push({

            id: "DISPUTE",

            label: "Dispute",

            handler: "dispute",

            variant: "secondary",

        });

        actions.push({

            id: "CANCEL",

            label: "Cancel Session",

            handler: "cancel",

            variant: "destructive",

        });

    }

    break;

    case ExchangeMode.DIRECT_BARTER:

    this.buildBarterActions(
        session,
        isProvider,
        isRequester,
        actions
    );

    break;

    case ExchangeMode.HYBRID:

        this.buildBarterActions(
        session,
        isProvider,
        isRequester,
        actions
    );

     break;

}

return actions;

}

// static getCurrentStage(
//     session: Session
// ): string {

//     return "";

// }


private static buildBarterActions(
    session: Session,
    isProvider: boolean,
    isRequester: boolean,
    actions: SessionAction[]
): void {

    // ----------------------------
    // START SESSION
    // ----------------------------

    if (session.status === "CREATED") {

        if (
            isProvider &&
            !session.providerStarted
        ) {

            actions.push({
                id: "START",
                label: "Start Session",
                handler: "start",
                primary: true,
                variant: "default",
            });

        }

        if (
                isRequester &&
                !session.requesterStarted
            ) {

            actions.push({
                id: "START",
                label: "Start Session",
                handler: "start",
                primary: true,
                variant: "default",
            });

        }

    }

    // ----------------------------
    // MARK WORK COMPLETED
    // ----------------------------

    if (session.status === "IN_PROGRESS") {

        if (
            isProvider &&
            !session.providerCompletedAt
        ) {

            actions.push({
                id: "MARK_COMPLETED",
                label: "Mark Work Completed",
                handler: "complete",
                primary: true,
                variant: "default",
            });

        }

        if (
            isRequester &&
            !session.requesterCompletedAt
        ) {

            actions.push({
                id: "MARK_COMPLETED",
                label: "Mark Work Completed",
                handler: "complete",
                primary: true,
                variant: "default",
            });

        }

    }

    // ----------------------------
    // CONFIRM COMPLETION
    // ----------------------------

    if (
        session.status === "IN_PROGRESS" &&
        isProvider &&
        session.requesterCompletedAt &&
        !session.providerConfirmed
    ) {

        actions.push({
            id: "CONFIRM_COMPLETION",
            label: "Confirm Completion",
            handler: "confirm",
            primary: true,
            variant: "default",
        });

    }

    if (
        session.status === "IN_PROGRESS" &&
        isRequester &&
        session.providerCompletedAt &&
        !session.requesterConfirmed
    ) {

        actions.push({
            id: "CONFIRM_COMPLETION",
            label: "Confirm Completion",
            handler: "confirm",
            primary: true,
            variant: "default",
        });

    }

    // ----------------------------
    // COMMON ACTIONS
    // ----------------------------

    if (
        session.status === "CREATED" ||
        session.status === "IN_PROGRESS"
    ) {

        actions.push({
            id: "DISPUTE",
            label: "Dispute",
            handler: "dispute",
            variant: "secondary",
        });

        actions.push({
            id: "CANCEL",
            label: "Cancel Session",
            handler: "cancel",
            variant: "destructive",
        });

    }

}


}