import { useState } from "react";

import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  Coins,
  RefreshCw,
} from "lucide-react";

import { Button } from "@/components/ui/button";

import type Agreement from "@/models/agreement/Agreement";

import type AgreementVersion from
  "@/models/agreement/AgreementVersion";

import type {
  AgreementMilestoneStatus,
} from "@/models/agreement/AgreementVersion";

import type Session from "@/models/sessions/Session";

import type {
  MilestoneCompletionResult,
} from "@/services/MilestoneService";

interface MilestoneWorkflowCardProps {
  agreement: Agreement | null;
  session: Session | null;
  activeVersion: AgreementVersion | null;
  userId?: string;
  onConfirmCompletion: (
    milestoneId: string,
  ) => Promise<MilestoneCompletionResult | null>;
  onRetryNextFunding: (
    agreementId: string,
  ) => Promise<void>;
}

const statusLabel: Record<AgreementMilestoneStatus, string> = {
  PLANNED: "Planned",
  FUNDING_PENDING: "Funding pending",
  FUNDING_FAILED: "Funding failed",
  FUNDED: "Funded",
  SUBMITTED: "Submitted",
  CORRECTION_REQUESTED: "Correction requested",
  DISPUTED: "Disputed",
  RELEASE_PENDING: "Release pending verification",
  RELEASED: "Released",
  REFUND_PENDING: "Refund pending",
  REFUNDED: "Refunded",
  CANCELLED: "Cancelled",
};

function MilestoneWorkflowCard({
  agreement,
  session,
  activeVersion,
  userId,
  onConfirmCompletion,
  onRetryNextFunding,
}: MilestoneWorkflowCardProps) {
  const [latestConfirmation, setLatestConfirmation] =
    useState<MilestoneCompletionResult | null>(null);

  const [busyAction, setBusyAction] =
    useState<string | null>(null);

  if (agreement?.releaseMode !== "MILESTONE_RELEASE") {
    return null;
  }

  const milestones = (
    activeVersion?.milestones ?? []
  )
    .slice()
    .sort(
      (a, b) =>
        a.milestoneNumber - b.milestoneNumber,
    );

  const isRequester =
    !!session && userId === session.requesterId;

  const isProvider =
    !!session && userId === session.providerId;

  const isParticipant = isRequester || isProvider;

  const allReleased =
    milestones.length > 0 &&
    milestones.every(
      (milestone) =>
        milestone.executionStatus === "RELEASED",
    );

  const currentIndex = milestones.findIndex(
    (milestone) =>
      milestone.executionStatus !== "RELEASED" &&
      milestone.executionStatus !== "CANCELLED",
  );

  const currentMilestone =
    currentIndex >= 0
      ? milestones[currentIndex]
      : null;

  const previousMilestonesReleased =
    currentIndex > 0 &&
    milestones
      .slice(0, currentIndex)
      .every(
        (milestone) =>
          milestone.executionStatus === "RELEASED",
      );

  const needsNextFunding =
    !!currentMilestone &&
    currentMilestone.milestoneNumber > 1 &&
    previousMilestonesReleased &&
    (
      currentMilestone.executionStatus === "PLANNED" ||
      currentMilestone.executionStatus === "FUNDING_PENDING"
    );

  const localConfirmed =
    !!currentMilestone &&
    latestConfirmation?.milestoneId ===
      currentMilestone.milestoneId &&
    (
      isRequester
        ? latestConfirmation.requesterConfirmed
        : isProvider &&
          latestConfirmation.providerConfirmed
    );

  const handleConfirm = async (
    milestoneId: string,
  ) => {
    setBusyAction(`confirm:${milestoneId}`);

    try {
      const result =
        await onConfirmCompletion(milestoneId);

      if (result) {
        setLatestConfirmation(result);
      }
    } finally {
      setBusyAction(null);
    }
  };

  const handleFundingRecovery = async () => {
    if (!agreement?.agreementId) {
      return;
    }

    setBusyAction("fund");

    try {
      await onRetryNextFunding(agreement.agreementId);
    } finally {
      setBusyAction(null);
    }
  };

  return (
    <section className="space-y-4 rounded-xl border p-4">
      <div className="flex items-start gap-3">
        <Coins
          size={20}
          className="mt-0.5 text-primary"
        />

        <div>
          <h3 className="font-semibold">
            Milestone release
          </h3>

          <p className="mt-1 text-xs text-muted-foreground">
            Each milestone requires confirmation from both
            participants before its escrow can be released.
            The next milestone is funded only after the
            preceding release is confirmed.
          </p>
        </div>
      </div>

      {!activeVersion ? (
        <div className="flex gap-2 rounded-lg bg-muted/40 p-3 text-xs text-muted-foreground">
          <AlertTriangle
            size={16}
            className="shrink-0"
          />

          The active milestone plan could not be loaded.
          Refresh the workspace before confirming a milestone.
        </div>
      ) : (
        <>
          <div className="space-y-1 text-xs text-muted-foreground">
            <p>
              Agreement version {activeVersion.versionNumber}
              {" · "}
              {activeVersion.status.replaceAll("_", " ")}
            </p>

            <p>
              Total agreed: {activeVersion.agreedTokenAmount} BLT
            </p>
          </div>

          {milestones.length > 0 && (
            <div className="space-y-3">
              {milestones.map((milestone) => {
                const released =
                  milestone.executionStatus === "RELEASED";

                return (
                  <div
                    key={milestone.versionMilestoneId}
                    className="space-y-2 rounded-lg border p-3"
                  >
                    <div className="flex items-start justify-between gap-2">
                      <div className="flex min-w-0 gap-2">
                        {released ? (
                          <CheckCircle2
                            size={16}
                            className="mt-0.5 shrink-0 text-green-600"
                          />
                        ) : (
                          <Clock
                            size={16}
                            className="mt-0.5 shrink-0 text-muted-foreground"
                          />
                        )}

                        <div>
                          <p className="text-sm font-medium">
                            {milestone.milestoneNumber}.{" "}
                            {milestone.title}
                          </p>

                          <p className="mt-1 text-xs text-muted-foreground">
                            {milestone.deliverable}
                          </p>
                        </div>
                      </div>

                      <span className="shrink-0 rounded-full bg-muted px-2 py-1 text-[10px]">
                        {statusLabel[milestone.executionStatus]}
                      </span>
                    </div>

                    <div className="flex flex-wrap justify-between gap-2 text-xs text-muted-foreground">
                      <span>
                        {milestone.tokenAllocation} BLT
                      </span>

                      <span>
                        Due{" "}
                        {new Date(
                          milestone.deadline,
                        ).toLocaleString()}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {currentMilestone?.executionStatus === "FUNDED" &&
            session?.status === "IN_PROGRESS" &&
            isParticipant &&
            !localConfirmed && (
              <Button
                className="w-full"
                onClick={() =>
                  handleConfirm(
                    currentMilestone.milestoneId,
                  )
                }
                disabled={busyAction !== null}
              >
                {busyAction ===
                `confirm:${currentMilestone.milestoneId}`
                  ? "Recording confirmation..."
                  : `Confirm Milestone ${currentMilestone.milestoneNumber} Completion`}
              </Button>
            )}

          {currentMilestone?.executionStatus === "FUNDED" &&
            session?.status === "IN_PROGRESS" &&
            localConfirmed && (
              <p className="rounded-lg bg-muted/40 p-3 text-xs text-muted-foreground">
                Your confirmation has been recorded.
                Waiting for the other participant to confirm.
              </p>
            )}

          {currentMilestone?.executionStatus ===
            "RELEASE_PENDING" && (
              <div className="space-y-2 rounded-lg bg-muted/40 p-3">
                <p className="text-xs text-muted-foreground">
                  Both confirmations are recorded. The
                  blockchain release still needs verification.
                </p>

                {isParticipant && (
                  <Button
                    variant="outline"
                    className="w-full"
                    onClick={() =>
                      handleConfirm(
                        currentMilestone.milestoneId,
                      )
                    }
                    disabled={busyAction !== null}
                  >
                    <RefreshCw
                      size={15}
                      className="mr-2"
                    />

                    {busyAction ===
                    `confirm:${currentMilestone.milestoneId}`
                      ? "Checking release..."
                      : "Check release status"}
                  </Button>
                )}
              </div>
            )}

          {needsNextFunding &&
            isRequester &&
            session?.status === "IN_PROGRESS" && (
              <div className="space-y-2 rounded-lg bg-muted/40 p-3">
                <p className="text-xs text-muted-foreground">
                  The preceding milestone has been released.
                  Check or recover funding for Milestone{" "}
                  {currentMilestone?.milestoneNumber}.
                  Only the requester can use this action.
                </p>

                <Button
                  className="w-full"
                  onClick={handleFundingRecovery}
                  disabled={busyAction !== null}
                >
                  {busyAction === "fund"
                    ? "Checking funding..."
                    : `Check Funding for Milestone ${currentMilestone?.milestoneNumber}`}
                </Button>
              </div>
            )}

          {currentMilestone?.executionStatus ===
            "FUNDING_FAILED" && (
              <p className="rounded-lg bg-muted/40 p-3 text-xs text-muted-foreground">
                Funding requires reconciliation. Do not submit
                another transaction until its blockchain outcome
                has been verified.
              </p>
            )}

          {allReleased &&
            isRequester &&
            session?.status !== "COMPLETED" && (
              <div className="space-y-2 rounded-lg bg-muted/40 p-3">
                <p className="text-xs text-muted-foreground">
                  Every milestone reports Released. Check
                  final agreement and session completion.
                </p>

                <Button
                  className="w-full"
                  onClick={handleFundingRecovery}
                  disabled={busyAction !== null}
                >
                  {busyAction === "fund"
                    ? "Checking completion..."
                    : "Retry Final Session Completion"}
                </Button>
              </div>
            )}

          {milestones.length === 0 && (
            <p className="text-xs text-muted-foreground">
              No milestone details are available on this version.
            </p>
          )}
        </>
      )}
    </section>
  );
}

export default MilestoneWorkflowCard;