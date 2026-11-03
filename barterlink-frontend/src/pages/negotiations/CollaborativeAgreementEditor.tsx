
import { useEffect, useState } from "react";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

import type AgreementVersion from "@/models/agreement/AgreementVersion";

import {
  submitAgreementVersion,
} from "@/services/AgreementVersionService";

import type {
  SubmitAgreementVersionPayload,
} from "@/services/AgreementVersionService";

import {
  AgreementYjsDocumentService,
} from "@/services/AgreementYjsDocumentService";

import type {
  AgreementYjsMilestone,
} from "@/services/AgreementYjsDocumentService";

import {
  useCollaborativeTextField,
} from "@/hooks/useCollaborativeTextField";

interface CollaborativeAgreementEditorProps {
  negotiationId: string;
  version: AgreementVersion;
  onSubmitted: () => Promise<void>;
}

type EditorStatus =
  | "loading"
  | "connected"
  | "saving"
  | "saved"
  | "disconnected"
  | "error";

function toLocalDateTimeInput(value?: string | null): string {
  if (!value) {
    return "";
  }

  const date = new Date(value);

  if (!Number.isFinite(date.getTime())) {
    return "";
  }

  date.setMinutes(
    date.getMinutes() - date.getTimezoneOffset(),
  );

  return date.toISOString().slice(0, 16);
}

function toInstant(value: string): string {
  return new Date(value).toISOString();
}

interface CollaborativeMilestoneEditorProps {
  manager: AgreementYjsDocumentService;
  milestone: AgreementYjsMilestone;
}

function CollaborativeMilestoneEditor({
  manager,
  milestone,
}: CollaborativeMilestoneEditorProps) {
  const title = useCollaborativeTextField({
    manager,
    milestoneId: milestone.milestoneId,
    field: "title",
    fallbackValue: milestone.title,
  });

  const deliverable = useCollaborativeTextField({
    manager,
    milestoneId: milestone.milestoneId,
    field: "deliverable",
    fallbackValue: milestone.deliverable,
  });

  const deadline = useCollaborativeTextField({
    manager,
    milestoneId: milestone.milestoneId,
    field: "deadline",
    fallbackValue: milestone.deadline,
  });

  const tokenAllocation = useCollaborativeTextField({
    manager,
    milestoneId: milestone.milestoneId,
    field: "tokenAllocation",
    fallbackValue: String(milestone.tokenAllocation),
  });

  return (
    <div className="space-y-3 rounded-lg border p-4">
      <p className="font-semibold">
        Milestone {milestone.milestoneNumber}
      </p>

      <div className="space-y-1">
        <Label>Milestone title</Label>

        <Input
          value={title.value}
          onChange={(event) =>
            title.onChange(event.target.value)
          }
          maxLength={255}
        />
      </div>

      <div className="space-y-1">
        <Label>Deliverable and acceptance criteria</Label>

        <textarea
          value={deliverable.value}
          onChange={(event) =>
            deliverable.onChange(event.target.value)
          }
          rows={3}
          className="w-full rounded-md border bg-background px-3 py-2 text-sm"
        />
      </div>

      <div className="space-y-1">
        <Label>Milestone deadline</Label>

        <Input
          type="datetime-local"
          value={toLocalDateTimeInput(deadline.value)}
          onChange={(event) =>
            deadline.onChange(
              event.target.value
                ? toInstant(event.target.value)
                : "",
            )
          }
        />
      </div>

      <div className="space-y-1">
        <Label>Token allocation</Label>

        <Input
          type="number"
          min={1}
          step={1}
          value={tokenAllocation.value}
          onChange={(event) =>
            tokenAllocation.onChange(event.target.value)
          }
        />
      </div>
    </div>
  );
}

export default function CollaborativeAgreementEditor({
  negotiationId,
  version,
  onSubmitted,
}: CollaborativeAgreementEditorProps) {
  const [manager, setManager] =
    useState<AgreementYjsDocumentService | null>(null);

  const [status, setStatus] =
    useState<EditorStatus>("loading");

  const [initializationError, setInitializationError] =
    useState<string | null>(null);

  const [submitting, setSubmitting] =
    useState(false);

  useEffect(() => {
    let cancelled = false;

    let openedManager:
      AgreementYjsDocumentService | null = null;

    const initialize = async () => {
      try {
        setStatus("loading");
        setInitializationError(null);

        const documentManager =
          await AgreementYjsDocumentService.open(
            negotiationId,
            version,
            {
              onStatusChange: (nextStatus) => {
                if (!cancelled) {
                  setStatus(nextStatus);
                }
              },

              onError: (error) => {
                if (!cancelled) {
                  console.error(
                    "Agreement document synchronization error:",
                    error,
                  );

                  toast.error(
                    "Could not synchronize the agreement document.",
                  );
                }
              },
            },
          );

        openedManager = documentManager;

        if (cancelled) {
          await documentManager.destroy();
          return;
        }

        setManager(documentManager);

      } catch (error) {
        console.error(
          "Agreement collaboration initialization failed:",
          error,
        );

        if (!cancelled) {
          setInitializationError(
            error instanceof Error
              ? error.message
              : "Could not open the shared agreement.",
          );

          setStatus("error");
        }
      }
    };

    void initialize();

    return () => {
      cancelled = true;

      if (openedManager) {
        void openedManager.destroy();
      }
    };
  }, [
    negotiationId,
    version.versionId,
  ]);

  const agreedTerms = useCollaborativeTextField({
    manager,
    field: "agreedTerms",
    fallbackValue: version.agreedTerms ?? "",
  });

  const agreedDeadline = useCollaborativeTextField({
    manager,
    field: "agreedDeadline",
    fallbackValue: version.agreedDeadline,
  });

  const changeSummary = useCollaborativeTextField({
    manager,
    field: "changeSummary",
    fallbackValue: version.changeSummary,
  });

  const getStatusLabel = () => {
    switch (status) {
      case "loading":
        return "Opening shared agreement...";

      case "connected":
        return "Connected";

      case "saving":
        return "Saving changes...";

      case "saved":
        return "All local changes synchronized";

      case "disconnected":
        return "Connection interrupted";

      case "error":
        return "Synchronization error";

      default:
        return "Unknown status";
    }
  };

  const getStatusClass = () => {
    switch (status) {
      case "connected":
      case "saved":
        return "text-emerald-600";

      case "saving":
        return "text-amber-600";

      case "disconnected":
      case "error":
        return "text-destructive";

      default:
        return "text-muted-foreground";
    }
  };

  const handleSubmit = async () => {
    if (!manager) {
      toast.error(
        "The shared agreement is not ready.",
      );
      return;
    }

    if (
      status !== "connected" &&
      status !== "saved"
    ) {
      toast.error(
        "Please wait until all your local edits are synchronized.",
      );
      return;
    }

    const snapshot = manager.getSnapshot();

    const deadlineTime =
      new Date(snapshot.agreedDeadline).getTime();

    if (
      !snapshot.agreedDeadline ||
      !Number.isFinite(deadlineTime) ||
      deadlineTime <= Date.now()
    ) {
      toast.error(
        "Enter a valid future agreement deadline.",
      );
      return;
    }

    if (!snapshot.changeSummary.trim()) {
      toast.error(
        "Enter an agreement summary.",
      );
      return;
    }

    let milestonePayload:
      SubmitAgreementVersionPayload["milestones"] = [];

    if (version.releaseMode === "MILESTONE_RELEASE") {
      if (
        snapshot.milestones.length !==
        version.milestones.length
      ) {
        toast.error(
          "The milestone count does not match the agreement draft.",
        );
        return;
      }

      let totalAllocation = 0;

      const sortedMilestones = [
        ...snapshot.milestones,
      ].sort(
        (a, b) =>
          a.milestoneNumber - b.milestoneNumber,
      );

      for (const milestone of sortedMilestones) {
        const allocation =
          milestone.tokenAllocation;

        const milestoneTime =
          new Date(milestone.deadline).getTime();

        if (
          !milestone.title.trim() ||
          !milestone.deliverable.trim()
        ) {
          toast.error(
            `Complete milestone ${milestone.milestoneNumber}.`,
          );
          return;
        }

        if (
          !Number.isInteger(allocation) ||
          allocation <= 0
        ) {
          toast.error(
            `Milestone ${milestone.milestoneNumber} requires a positive whole-token allocation.`,
          );
          return;
        }

        if (
          !Number.isFinite(milestoneTime) ||
          milestoneTime <= Date.now() ||
          milestoneTime > deadlineTime
        ) {
          toast.error(
            `Milestone ${milestone.milestoneNumber} must have a valid future deadline no later than the agreement deadline.`,
          );
          return;
        }

        totalAllocation += allocation;
      }

      if (
        totalAllocation !== version.agreedTokenAmount
      ) {
        toast.error(
          `Milestone allocations must total exactly ${version.agreedTokenAmount} tokens.`,
        );
        return;
      }

      milestonePayload = sortedMilestones.map(
        (milestone) => ({
          milestoneId: milestone.milestoneId,
          milestoneNumber: milestone.milestoneNumber,
          title: milestone.title.trim(),
          deliverable: milestone.deliverable.trim(),
          deadline: toInstant(milestone.deadline),
          tokenAllocation: milestone.tokenAllocation,
        }),
      );
    }

    const payload: SubmitAgreementVersionPayload = {
      agreedTerms:
        snapshot.agreedTerms.trim() || undefined,

      agreedDeadline:
        toInstant(snapshot.agreedDeadline),

      changeSummary:
        snapshot.changeSummary.trim(),

      milestones: milestonePayload,
    };

    try {
      setSubmitting(true);

      await submitAgreementVersion(
        negotiationId,
        version.versionId,
        payload,
      );

      toast.success(
        "Agreement submitted. Only the other participant needs to accept it.",
      );

      await onSubmitted();

    } catch (error: unknown) {
      const message =
        error &&
        typeof error === "object" &&
        "response" in error
          ? (
              error as {
                response?: {
                  data?: { message?: string };
                };
              }
            ).response?.data?.message
          : undefined;

      toast.error(
        message ??
          "Could not submit the collaborative agreement.",
      );

    } finally {
      setSubmitting(false);
    }
  };

  if (initializationError) {
    return (
      <div className="space-y-2 rounded-lg border border-destructive/40 p-4">
        <p className="font-medium text-destructive">
          Could not open shared agreement
        </p>

        <p className="text-sm text-muted-foreground">
          {initializationError}
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4 rounded-lg border border-primary/30 p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div>
          <h4 className="font-semibold">
            Shared agreement editor
          </h4>

          <p className="text-xs text-muted-foreground">
            Both participants can edit this agreement simultaneously.
          </p>
        </div>

        <span
          className={`text-xs font-medium ${getStatusClass()}`}
        >
          {getStatusLabel()}
        </span>
      </div>

      {!manager ? (
        <p className="text-sm text-muted-foreground">
          Loading the shared agreement document...
        </p>
      ) : (
        <>
          <div className="space-y-1">
            <Label>Agreement summary</Label>

            <Input
              value={changeSummary.value}
              onChange={(event) =>
                changeSummary.onChange(
                  event.target.value,
                )
              }
              maxLength={255}
            />
          </div>

          <div className="space-y-1">
            <Label>Agreement deadline</Label>

            <Input
              type="datetime-local"
              value={toLocalDateTimeInput(
                agreedDeadline.value,
              )}
              onChange={(event) =>
                agreedDeadline.onChange(
                  event.target.value
                    ? toInstant(event.target.value)
                    : "",
                )
              }
            />
          </div>

          <div className="space-y-1">
            <Label>
              Agreed terms and additional conditions
            </Label>

            <textarea
              rows={6}
              value={agreedTerms.value}
              onChange={(event) =>
                agreedTerms.onChange(
                  event.target.value,
                )
              }
              maxLength={10000}
              placeholder="Both participants can write and modify the agreement terms here."
              className="w-full rounded-md border bg-background px-3 py-2 text-sm"
            />
          </div>

          {version.releaseMode === "MILESTONE_RELEASE" && (
            <div className="space-y-3">
              <div>
                <h4 className="font-semibold">
                  Shared milestone plan
                </h4>

                <p className="text-xs text-muted-foreground">
                  Both participants can edit milestone details.
                  Total allocation must remain equal to the
                  agreed token amount.
                </p>
              </div>

              {version.milestones
                .slice()
                .sort(
                  (a, b) =>
                    a.milestoneNumber -
                    b.milestoneNumber,
                )
                .map((milestone) => (
                  <CollaborativeMilestoneEditor
                    key={milestone.versionMilestoneId}
                    manager={manager}
                    milestone={{
                      milestoneId: milestone.milestoneId,
                      milestoneNumber:
                        milestone.milestoneNumber,
                      title: milestone.title,
                      deliverable: milestone.deliverable,
                      deadline: milestone.deadline,
                      tokenAllocation:
                        milestone.tokenAllocation,
                    }}
                  />
                ))}
            </div>
          )}

          <div className="rounded-md bg-muted/40 p-3 text-xs text-muted-foreground">
            Exchange mode: {version.exchangeMode}
            <br />
            Release mode: {version.releaseMode ?? "Not applicable"}
            <br />
            Agreed token amount: {version.agreedTokenAmount}
          </div>

          <div className="flex flex-wrap justify-end gap-2">
            <Button
              onClick={handleSubmit}
              disabled={
                submitting ||
                !manager ||
                (status !== "connected" &&
                  status !== "saved")
              }
            >
              {submitting
                ? "Submitting..."
                : "Submit agreement for acceptance"}
            </Button>
          </div>

          <p className="text-xs text-muted-foreground">
            Submitting locks this version. The submitting
            participant is automatically confirmed, and the
            other participant must accept it.
          </p>
        </>
      )}
    </div>
  );
}
