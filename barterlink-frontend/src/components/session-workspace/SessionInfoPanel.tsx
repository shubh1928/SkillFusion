import type Agreement from "@/models/agreement/Agreement";
import type CollaborationRoom from "@/models/collaboration/CollaborationRoom";
import type Session from "@/models/sessions/Session";

import StatusCard from "./info-panel/StatusCard";
import CollaborationRoomCard from "./info-panel/CollaborationRoomCard";
import TimerCard from "./info-panel/TimerCard";

import ProgressCard from "./info-panel/ProgressCard";
import ParticipantsCard from "./info-panel/ParticipantsCard";
import AgreementCard from "./info-panel/AgreementCard";

import type AgreementVersion from
  "@/models/agreement/AgreementVersion";

import type {
  MilestoneCompletionResult,
} from "@/services/MilestoneService";

import MilestoneWorkflowCard from
  "./info-panel/MilestoneWorkflowCard";

import SessionActionsCard
from "./info-panel/SessionActionsCard";

interface SessionInfoPanelProps {
    session: Session | null;
    agreement: Agreement | null;
    collaborationRoom: CollaborationRoom | null;
    elapsedTime: string;

    activeVersion: AgreementVersion | null;

onConfirmMilestoneCompletion: (
    milestoneId: string,
) => Promise<MilestoneCompletionResult | null>;

onRetryNextMilestoneFunding: (
    agreementId: string,
) => Promise<void>;

    userId?: string;

    onStartSession: () => void;
    onProviderComplete: () => void;
    onRequesterComplete: () => void;
    onCancelSession: () => void;
    onDisputeSession: () => void;
}

function SessionInfoPanel({
    session,
    agreement,
    collaborationRoom,
    elapsedTime,

    activeVersion,
onConfirmMilestoneCompletion,
onRetryNextMilestoneFunding,

    userId,

    onStartSession,
    onProviderComplete,
    onRequesterComplete,
    onCancelSession,
    onDisputeSession,
}: SessionInfoPanelProps) {

return (
    <div className="flex h-full min-h-0 flex-col overflow-hidden rounded-xl border">

        {/* Fixed header */}
        <div className="shrink-0 border-b p-5">
            <h2 className="text-xl font-bold">
                Session Details
            </h2>
        </div>

        {/* Independently scrollable content */}
        <div className="min-h-0 min-w-0 flex-1 space-y-6 overflow-y-auto overscroll-contain p-6">

            <StatusCard
                status={session?.status}
            />

            <CollaborationRoomCard
                collaborationRoom={collaborationRoom}
            />

            <TimerCard
                elapsedTime={elapsedTime}
                completed={!!session?.completedAt}
            />

            <ProgressCard
                session={session}
                milestoneRelease={
                    agreement?.releaseMode === "MILESTONE_RELEASE"
                }
            />

            <ParticipantsCard
                session={session}
            />

            <AgreementCard
                agreement={agreement}
            />

            <SessionActionsCard
                session={session}
                userId={userId}
                onStartSession={onStartSession}
                onProviderComplete={onProviderComplete}
                onRequesterComplete={onRequesterComplete}
                onCancelSession={onCancelSession}
                onDisputeSession={onDisputeSession}
                milestoneRelease={
                    agreement?.releaseMode === "MILESTONE_RELEASE"
                }
            />

            <MilestoneWorkflowCard
                agreement={agreement}
                session={session}
                activeVersion={activeVersion}
                userId={userId}
                onConfirmCompletion={onConfirmMilestoneCompletion}
                onRetryNextFunding={onRetryNextMilestoneFunding}
            />

        </div>
    </div>
);
}

export default SessionInfoPanel;