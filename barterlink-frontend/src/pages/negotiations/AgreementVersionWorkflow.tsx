import {
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import CollaborativeAgreementEditor from "@/pages/negotiations/CollaborativeAgreementEditor";
import type Negotiation from "@/models/negotiation/Negotiation";

import type AgreementVersion from "@/models/agreement/AgreementVersion";
import type {
  AgreementReleaseMode,
  AgreementVersionStatus,
  CreateAgreementVersionPayload,
} from "@/models/agreement/AgreementVersion";

import {
  acceptAgreementVersion,
  createAgreementVersionDraft,
  rejectAgreementVersion,
} from "@/services/AgreementVersionService";
import {
  useCollaborativeTextField,
} from "@/hooks/useCollaborativeTextField";

import {
  AgreementYjsDocumentService,
} from "@/services/AgreementYjsDocumentService";

import type {
  AgreementYjsSnapshot,
} from "@/services/AgreementYjsDocumentService";
import { selectAgreementReleaseMode } from "@/services/NegotiationService";

interface AgreementVersionWorkflowProps {
  negotiation: Negotiation | null;
  versions: AgreementVersion[];
  currentUserId?: string;
  onRefresh: () => Promise<void>;
}

interface MilestoneDraft {
  milestoneId?: string;
  sourceVersionId?: string;
  title: string;
  deliverable: string;
  deadline: string;
  tokenAllocation: string;
}

const statusLabels: Record<AgreementVersionStatus, string> = {
  DRAFT: "Draft",
  PENDING_ACCEPTANCE: "Pending acceptance",
  READY_FOR_ACTIVATION: "Accepted by both",
  FUNDING_FAILED: "Funding requires attention",
  ACTIVE: "Active",
  SUPERSEDED: "Superseded",
  REJECTED: "Rejected",
};



function localDateTimeDaysFromNow(days: number): string {
  const date = new Date(
    Date.now() + days * 24 * 60 * 60 * 1000,
  );

  date.setMinutes(
    date.getMinutes() - date.getTimezoneOffset(),
  );

  return date.toISOString().slice(0, 16);
}

function toInstant(value: string): string {
  return new Date(value).toISOString();
}

function toLocalDateTimeInput(
  value?: string | null,
): string {
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

function createMilestoneDrafts(
  count: number,
  totalTokens: number,
): MilestoneDraft[] {
  const baseAllocation = Math.floor(totalTokens / count);
  const remainder = totalTokens % count;

  return Array.from({ length: count }, (_, index) => ({
  milestoneId: crypto.randomUUID(),
  title: `Milestone ${index + 1}`,
  deliverable: "",
  deadline: localDateTimeDaysFromNow(index + 1),
  tokenAllocation:
    totalTokens > 0
      ? String(
          baseAllocation + (index < remainder ? 1 : 0),
        )
      : "",
}));
}


interface CollaborativeCurrentDealMilestoneProps {
  manager: AgreementYjsDocumentService | null;
  milestone: MilestoneDraft;
  index: number;
  agreedDeadline: string;
  onUpdate: (
    index: number,
    field:
      | "title"
      | "deliverable"
      | "deadline"
      | "tokenAllocation",
    value: string,
  ) => void;
}

function CollaborativeCurrentDealMilestone({
  manager,
  milestone,
  index,
  agreedDeadline,
  onUpdate,
}: CollaborativeCurrentDealMilestoneProps) {
  const fieldManager = milestone.milestoneId
    ? manager
    : null;

  // Collaborative milestone title
  const title = useCollaborativeTextField({
    manager: fieldManager,
    milestoneId: milestone.milestoneId,
    field: "title",
    fallbackValue: milestone.title,
  });

  // Collaborative deliverable
  const deliverable = useCollaborativeTextField({
    manager: fieldManager,
    milestoneId: milestone.milestoneId,
    field: "deliverable",
    fallbackValue: milestone.deliverable,
  });

  // Collaborative deadline
  const deadline = useCollaborativeTextField({
    manager: fieldManager,
    milestoneId: milestone.milestoneId,
    field: "deadline",
    fallbackValue: milestone.deadline
      ? toInstant(milestone.deadline)
      : "",
  });

  // Collaborative token allocation
  const tokenAllocation = useCollaborativeTextField({
    manager: fieldManager,
    milestoneId: milestone.milestoneId,
    field: "tokenAllocation",
    fallbackValue: milestone.tokenAllocation,
  });

  return (
    <div className="space-y-3 rounded-lg border p-3">
      <p className="text-sm font-semibold">
        Milestone {index + 1}
      </p>

      {/* Milestone title */}
      <div className="space-y-1">
        <Label htmlFor={`milestone-title-${index}`}>
          Title
        </Label>

        <Input
          id={`milestone-title-${index}`}
          value={title.value}
          onChange={(event) => {
            const value = event.target.value;

            onUpdate(index, "title", value);
            title.onChange(value);
          }}
          maxLength={255}
        />
      </div>

      {/* Milestone deliverable */}
      <div className="space-y-1">
        <Label
          htmlFor={`milestone-deliverable-${index}`}
        >
          Deliverable and acceptance criteria
        </Label>

        <textarea
          id={`milestone-deliverable-${index}`}
          rows={3}
          value={deliverable.value}
          onChange={(event) => {
            const value = event.target.value;

            onUpdate(index, "deliverable", value);
            deliverable.onChange(value);
          }}
          className="w-full rounded-md border bg-background px-3 py-2 text-sm"
        />
      </div>

      {/* Milestone deadline */}
      <div className="space-y-1">
        <Label htmlFor={`milestone-deadline-${index}`}>
          Deadline
        </Label>

        <Input
          id={`milestone-deadline-${index}`}
          type="datetime-local"
          value={toLocalDateTimeInput(deadline.value)}
          max={agreedDeadline || undefined}
          onChange={(event) => {
            const value = event.target.value;

            onUpdate(index, "deadline", value);

            deadline.onChange(
              value ? toInstant(value) : "",
            );
          }}
        />
      </div>

      {/* Token allocation */}
      <div className="space-y-1">
        <Label
          htmlFor={`milestone-allocation-${index}`}
        >
          BLT allocation
        </Label>

        <Input
          id={`milestone-allocation-${index}`}
          type="number"
          min={1}
          step={1}
          value={tokenAllocation.value}
          onChange={(event) => {
            const value = event.target.value;

            onUpdate(
              index,
              "tokenAllocation",
              value,
            );

            tokenAllocation.onChange(value);
          }}
        />
      </div>
    </div>
  );
}




function AgreementVersionWorkflow({
  negotiation,
  versions,
  currentUserId,
  onRefresh,
}: AgreementVersionWorkflowProps) {
  const [releaseMode, setReleaseMode] =
    useState<AgreementReleaseMode>("DIRECT_RELEASE");

  const [milestoneCount, setMilestoneCount] =
    useState(3);

  const [savingChoice, setSavingChoice] =
    useState(false);

  const [creatingDraft, setCreatingDraft] =
    useState(false);

  const [agreedDeadline, setAgreedDeadline] =
    useState(() => localDateTimeDaysFromNow(7));

  const [changeSummary, setChangeSummary] =
    useState("Initial agreement version");

  const [agreedTerms, setAgreedTerms] = useState("");

  const [milestones, setMilestones] =
    useState<MilestoneDraft[]>([]);

    const [agreementManager, setAgreementManager] =
  useState<AgreementYjsDocumentService | null>(null);

const [agreementSyncStatus, setAgreementSyncStatus] =
  useState<
    | "idle"
    | "loading"
    | "connected"
    | "saving"
    | "saved"
    | "disconnected"
    | "error"
  >("idle");

const [draftFormInitializedKey, setDraftFormInitializedKey] =
  useState<string | null>(null);

  const [pendingActionId, setPendingActionId] =
    useState<string | null>(null);

  const isRequester =
    !!negotiation &&
    currentUserId === negotiation.requesterId;

  const isProvider =
    !!negotiation &&
    currentUserId === negotiation.providerId;

  const ownMode = isRequester
    ? negotiation?.requesterReleaseMode
    : negotiation?.providerReleaseMode;

  const ownCount = isRequester
    ? negotiation?.requesterMilestoneCount
    : negotiation?.providerMilestoneCount;

  const otherCount = isRequester
    ? negotiation?.providerMilestoneCount
    : negotiation?.requesterMilestoneCount;

  const selectionRequired =
    negotiation?.releaseModeSelectionRequired === true ||
    negotiation?.requesterReleaseMode != null ||
    negotiation?.providerReleaseMode != null;

  const tokenEligible =
  negotiation?.exchangeMode === "TOKEN_BASED" ||
  negotiation?.exchangeMode === "HYBRID";

const directBarter =
  negotiation?.exchangeMode === "DIRECT_BARTER";

  const activeVersion =
  versions.find(
    (version) => version.status === "ACTIVE",
  ) ?? null;

const activeVersionId = activeVersion?.versionId;
const activeVersionNumber = activeVersion?.versionNumber;
const activeVersionDeadline = activeVersion?.agreedDeadline;
const activeVersionTerms = activeVersion?.agreedTerms;

  const modesMatch =
    !!negotiation?.requesterReleaseMode &&
    negotiation.requesterReleaseMode ===
      negotiation.providerReleaseMode;

  const agreedReleaseMode = modesMatch
    ? negotiation?.requesterReleaseMode
    : null;

  const countsMatch =
    agreedReleaseMode !== "MILESTONE_RELEASE" ||
    (
      negotiation?.requesterMilestoneCount != null &&
      negotiation.requesterMilestoneCount ===
        negotiation.providerMilestoneCount &&
      negotiation.requesterMilestoneCount >= 2 &&
      negotiation.requesterMilestoneCount <= 5
    );

  const releaseChoiceAgreed =
    modesMatch && countsMatch;

  const planCount =
    releaseChoiceAgreed &&
    agreedReleaseMode === "MILESTONE_RELEASE"
      ? negotiation?.requesterMilestoneCount ?? 0
      : 0;

  const unresolvedVersionExists = versions.some(
    (version) =>
      version.status === "DRAFT" ||
      version.status === "PENDING_ACCEPTANCE" ||
      version.status === "READY_FOR_ACTIVATION",
  );

const canCreateDraft =
  !!negotiation &&
  (
    negotiation.status === "ACTIVE" ||
    negotiation.status === "SESSION_REQUESTED" ||
    negotiation.status === "ACCEPTED"
  ) &&
  (directBarter || releaseChoiceAgreed) &&
  !unresolvedVersionExists &&
  (
    versions.length === 0 ||
    !!activeVersion
  ) &&
  versions.every(
    (version) =>
      version.status === "ACTIVE" ||
      version.status === "SUPERSEDED" ||
      version.status === "REJECTED" ||
      version.status === "FUNDING_FAILED",
  );

  const negotiationId = negotiation?.negotiationId;

const currentDraftFormKey = negotiation
  ? `${negotiation.negotiationId}:${activeVersionId ?? "initial"}`
  : null;

const milestonesReady =
  agreedReleaseMode !== "MILESTONE_RELEASE" ||
  (
    milestones.length === planCount &&
    milestones.every((milestone) => !!milestone.milestoneId)
  );

const canOpenSharedCurrentDeal =
  !!negotiation &&
  canCreateDraft &&
  !activeVersion &&
  milestonesReady &&
  draftFormInitializedKey === currentDraftFormKey;

  const currentDealSnapshotRef =
  useRef<AgreementYjsSnapshot>({
    agreedTerms,
    agreedDeadline: agreedDeadline
      ? toInstant(agreedDeadline)
      : "",
    changeSummary,
    milestones: [],
  });

currentDealSnapshotRef.current = {
  agreedTerms,
  agreedDeadline: agreedDeadline
    ? toInstant(agreedDeadline)
    : "",
  changeSummary,
  milestones:
    agreedReleaseMode === "MILESTONE_RELEASE"
      ? milestones.map((milestone, index) => ({
          milestoneId: milestone.milestoneId ?? "",
          milestoneNumber: index + 1,
          title: milestone.title,
          deliverable: milestone.deliverable,
          deadline: milestone.deadline
            ? toInstant(milestone.deadline)
            : "",
          tokenAllocation:
            Number(milestone.tokenAllocation) || 0,
        }))
      : [],
};

  const allocationTotal = useMemo(
    () =>
      milestones.reduce((sum, milestone) => {
        const amount = Number(milestone.tokenAllocation);

        return sum +
          (
            Number.isInteger(amount) && amount > 0
              ? amount
              : 0
          );
      }, 0),
    [milestones],
  );
  

  useEffect(() => {
    if (!negotiation) {
      return;
    }

    setReleaseMode(ownMode ?? "DIRECT_RELEASE");

    setMilestoneCount(
      ownCount ?? otherCount ?? 3,
    );
  }, [
    negotiation?.negotiationId,
    ownMode,
    ownCount,
    otherCount,
  ]);


useEffect(() => {
  if (!negotiation) {
    return;
  }

  if (activeVersionId) {
    setAgreedDeadline(
      toLocalDateTimeInput(activeVersionDeadline),
    );

    setChangeSummary(
      `Amendment to version ${activeVersionNumber}`,
    );

    setAgreedTerms(activeVersionTerms ?? "");

    setDraftFormInitializedKey(
  `${negotiation.negotiationId}:${activeVersionId}`,
);

    return;
  }

  setAgreedDeadline(localDateTimeDaysFromNow(7));
  setChangeSummary("Initial agreement version");
  setAgreedTerms("");

  setDraftFormInitializedKey(
  `${negotiation.negotiationId}:initial`,
);
}, [
  negotiation?.negotiationId,
  activeVersionId,
  activeVersionNumber,
  activeVersionDeadline,
  activeVersionTerms,
]);


useEffect(() => {
  if (
    agreedReleaseMode !== "MILESTONE_RELEASE" ||
    planCount < 2 ||
    planCount > 5
  ) {
    setMilestones([]);
    return;
  }

  if (activeVersion?.releaseMode === "MILESTONE_RELEASE") {
    setMilestones((current) => {
      const alreadyFromActiveVersion =
        current.length === activeVersion.milestones.length &&
        current.length > 0 &&
        current.every(
          (milestone) =>
            milestone.sourceVersionId === activeVersionId,
        );

      if (alreadyFromActiveVersion) {
        return current;
      }

      return [...activeVersion.milestones]
        .sort(
          (a, b) => a.milestoneNumber - b.milestoneNumber,
        )
        .map((milestone) => ({
          milestoneId: milestone.milestoneId,
          sourceVersionId: activeVersionId,
          title: milestone.title,
          deliverable: milestone.deliverable,
          deadline: toLocalDateTimeInput(milestone.deadline),
          tokenAllocation: String(milestone.tokenAllocation),
        }));
    });

    return;
  }

  setMilestones((current) => {
    if (
      current.length === planCount &&
      current.every((milestone) => !milestone.sourceVersionId)
    ) {
      return current;
    }

    return createMilestoneDrafts(
      planCount,
      negotiation?.tokenAmount ?? 0,
    );
  });
}, [
  agreedReleaseMode,
  planCount,
  negotiation?.tokenAmount,
  activeVersionId,
]);

useEffect(() => {
  if (
    !negotiationId ||
    !canOpenSharedCurrentDeal ||
    activeVersionId
  ) {
    setAgreementManager(null);
    setAgreementSyncStatus("idle");
    return;
  }

  let cancelled = false;

  let openedManager:
    AgreementYjsDocumentService | null = null;

  const initialize = async () => {
    try {
      setAgreementSyncStatus("loading");

      const manager =
        await AgreementYjsDocumentService.openNegotiationDraft(
          negotiationId,
          currentDealSnapshotRef.current,
          {
            onChange: (snapshot) => {
              if (cancelled) {
                return;
              }

              // Synchronize agreement deadline.
              setAgreedDeadline(
                toLocalDateTimeInput(
                  snapshot.agreedDeadline,
                ),
              );

              // Synchronize agreement summary.
              setChangeSummary(snapshot.changeSummary);

              // Synchronize agreed terms.
              setAgreedTerms(snapshot.agreedTerms);

              // Synchronize milestone details.
              setMilestones(
                snapshot.milestones.map((milestone) => ({
                  milestoneId: milestone.milestoneId,
                  title: milestone.title,
                  deliverable: milestone.deliverable,
                  deadline: toLocalDateTimeInput(
                    milestone.deadline,
                  ),
                  tokenAllocation:
                    milestone.tokenAllocation > 0
                      ? String(milestone.tokenAllocation)
                      : "",
                })),
              );
            },

            onStatusChange: (status) => {
              if (!cancelled) {
                setAgreementSyncStatus(status);
              }
            },

            onError: (error) => {
              if (!cancelled) {
                console.error(
                  "Current Deal collaboration error:",
                  error,
                );

                setAgreementSyncStatus("error");

                toast.error(
                  "Could not synchronize the Current Deal.",
                );
              }
            },
          },
        );

      openedManager = manager;

      if (cancelled) {
        await manager.destroy();
        return;
      }

      setAgreementManager(manager);

    } catch (error) {
      console.error(
        "Current Deal initialization failed:",
        error,
      );

      if (!cancelled) {
        setAgreementSyncStatus("error");

        toast.error(
          error instanceof Error
            ? error.message
            : "Could not initialize shared Current Deal.",
        );
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
  canOpenSharedCurrentDeal,
  activeVersionId,
]);


const agreedDeadlineField = useCollaborativeTextField({
  manager: agreementManager,
  field: "agreedDeadline",
  fallbackValue: agreedDeadline
    ? toInstant(agreedDeadline)
    : "",
});

const changeSummaryField = useCollaborativeTextField({
  manager: agreementManager,
  field: "changeSummary",
  fallbackValue: changeSummary,
});

const agreedTermsField = useCollaborativeTextField({
  manager: agreementManager,
  field: "agreedTerms",
  fallbackValue: agreedTerms,
});



  if (
  !negotiation ||
  (!directBarter && (!selectionRequired || !tokenEligible))
) {
  return null;
}

  const updateMilestone = (
    index: number,
    field: keyof MilestoneDraft,
    value: string,
  ) => {
    setMilestones((current) =>
      current.map((milestone, i) =>
        i === index
          ? { ...milestone, [field]: value }
          : milestone,
      ),
    );
  };

  const handleSaveChoice = async () => {
    if (
      releaseMode === "MILESTONE_RELEASE" &&
      (milestoneCount < 2 || milestoneCount > 5)
    ) {
      toast.error("Choose between 2 and 5 milestones.");
      return;
    }

    if (!negotiation) {
      return;
    }

    try {
      setSavingChoice(true);

      await selectAgreementReleaseMode(
        negotiation.negotiationId,
        releaseMode,
        releaseMode === "MILESTONE_RELEASE"
          ? milestoneCount
          : undefined,
      );

      toast.success(
        "Your release-mode choice has been saved.",
      );

      await onRefresh();
    } catch (error: any) {
      toast.error(
        error?.response?.data?.message ??
          "Could not save your release-mode choice.",
      );
    } finally {
      setSavingChoice(false);
    }
  };

const handleCreateDraft = async () => {
  if (!negotiation) {
    return;
  }

  /*
   * Read the latest shared Yjs values directly.
   * For amendments, continue using the existing React state.
   */
  const sharedSnapshot =
    !activeVersionId && agreementManager
      ? agreementManager.getSnapshot()
      : null;

const submittedAgreedDeadline =
  sharedSnapshot?.agreedDeadline ?? agreedDeadline;

  const submittedChangeSummary =
    sharedSnapshot?.changeSummary ?? changeSummary;

  const submittedAgreedTerms =
    sharedSnapshot?.agreedTerms ?? agreedTerms;

  const submittedMilestones: MilestoneDraft[] =
    sharedSnapshot
      ? sharedSnapshot.milestones.map((milestone) => ({
          milestoneId: milestone.milestoneId,
          title: milestone.title,
          deliverable: milestone.deliverable,
          deadline: toLocalDateTimeInput(
            milestone.deadline,
          ),
          tokenAllocation:
            milestone.tokenAllocation > 0
              ? String(milestone.tokenAllocation)
              : "",
        }))
      : milestones;

  const agreedDeadlineTime = new Date(
    submittedAgreedDeadline,
  ).getTime();

  /*
   * Make sure the shared document has finished saving
   * before creating the agreement version.
   */
if (!activeVersionId) {
  if (!agreementManager) {
    toast.error(
      "Current Deal collaboration is not ready. Please wait for synchronization.",
    );
    return;
  }

  const syncStatus = agreementManager.getStatus();

  if (
    syncStatus !== "connected" &&
    syncStatus !== "saved"
  ) {
    toast.error(
      "Wait until Current Deal changes are saved before creating the agreement draft.",
    );
    return;
  }
}

  // Validate agreement deadline.
  if (
    !submittedAgreedDeadline ||
    !Number.isFinite(agreedDeadlineTime) ||
    agreedDeadlineTime <= Date.now()
  ) {
    toast.error(
      "Enter a valid future agreement deadline.",
    );
    return;
  }

  // Validate agreement summary.
  if (!submittedChangeSummary.trim()) {
    toast.error("Enter an agreement summary.");
    return;
  }

  const isDirectBarter =
    negotiation.exchangeMode === "DIRECT_BARTER";

  const tokenAmount = isDirectBarter
    ? 0
    : (negotiation.tokenAmount ?? 0);

  if (
    !isDirectBarter &&
    (
      !Number.isInteger(tokenAmount) ||
      tokenAmount == null ||
      tokenAmount <= 0
    )
  ) {
    toast.error(
      "The agreed amount must be a positive whole-BLT amount.",
    );
    return;
  }

  // Validate estimated hours.
  if (
    !Number.isInteger(negotiation.estimatedHours) ||
    negotiation.estimatedHours <= 0
  ) {
    toast.error(
      "Estimated hours must be a positive whole number.",
    );
    return;
  }

  let milestonePayload:
    CreateAgreementVersionPayload["milestones"] = [];

  // Validate milestone-based release.
  if (
    !isDirectBarter &&
    agreedReleaseMode === "MILESTONE_RELEASE"
  ) {
    if (submittedMilestones.length !== planCount) {
      toast.error(
        `Enter all ${planCount} milestones.`,
      );
      return;
    }

    for (
      let index = 0;
      index < submittedMilestones.length;
      index++
    ) {
      const milestone = submittedMilestones[index];

      const amount = Number(
        milestone.tokenAllocation,
      );

      const milestoneTime = new Date(
        milestone.deadline,
      ).getTime();

      if (
        !milestone.title.trim() ||
        !milestone.deliverable.trim() ||
        !milestone.deadline
      ) {
        toast.error(
          `Complete the title, deliverable, and deadline for milestone ${index + 1}.`,
        );
        return;
      }

      if (
        !milestone.milestoneId
      ) {
        toast.error(
          `Milestone ${index + 1} is missing its shared identifier. Please reopen the agreement form.`,
        );
        return;
      }

      if (
        !Number.isInteger(amount) ||
        amount <= 0
      ) {
        toast.error(
          `Milestone ${index + 1} requires a positive whole-BLT allocation.`,
        );
        return;
      }

      if (
        !Number.isFinite(milestoneTime) ||
        milestoneTime <= Date.now() ||
        milestoneTime > agreedDeadlineTime
      ) {
        toast.error(
          `Milestone ${index + 1} deadline must be in the future and no later than the agreement deadline.`,
        );
        return;
      }
    }

    // Verify total milestone allocation.
    const total = submittedMilestones.reduce(
      (sum, milestone) =>
        sum + Number(milestone.tokenAllocation),
      0,
    );

    if (total !== tokenAmount) {
      toast.error(
        `Milestone allocations total ${total} BLT, but the agreement amount is ${tokenAmount} BLT. The amounts must match exactly.`,
      );
      return;
    }

    // Prepare milestone API payload.
    milestonePayload = submittedMilestones.map(
      (milestone, index) => ({
        milestoneId: milestone.milestoneId,
        milestoneNumber: index + 1,
        title: milestone.title.trim(),
        deliverable: milestone.deliverable.trim(),
        deadline: toInstant(milestone.deadline),
        tokenAllocation: Number(
          milestone.tokenAllocation,
        ),
      }),
    );
  }

  // Prepare final agreement payload.
  const payload: CreateAgreementVersionPayload = {
    exchangeMode: negotiation.exchangeMode,

    offerTitle:
      negotiation.offerTitle ?? undefined,

    offerDescription:
      negotiation.offerDescription ?? undefined,

    requestTitle: negotiation.requestTitle,

    requestDescription:
      negotiation.requestDescription,

    agreedTerms:
      submittedAgreedTerms.trim() || undefined,

    estimatedHours: negotiation.estimatedHours,

    agreedTokenAmount: tokenAmount,

    agreedDeadline: toInstant(
      submittedAgreedDeadline,
    ),

    changeSummary:
      submittedChangeSummary.trim(),

    milestones: milestonePayload,
  };

  try {
    setCreatingDraft(true);

    await createAgreementVersionDraft(
      negotiation.negotiationId,
      payload,
    );

    toast.success(
      "Agreement draft created. Submit it for acceptance next.",
    );

    await onRefresh();

  } catch (error: any) {
    toast.error(
      error?.response?.data?.message ??
        "Could not create the agreement version.",
    );

  } finally {
    setCreatingDraft(false);
  }
};

const runVersionAction = async (
  version: AgreementVersion,
  action: "accept" | "reject",
) => {
  if (!negotiation) {
    return;
  }

  const actionId = `${version.versionId}:${action}`;

  setPendingActionId(actionId);

  try {
    if (action === "accept") {
      await acceptAgreementVersion(
        negotiation.negotiationId,
        version.versionId,
      );

      toast.success(
        "Your acceptance has been recorded.",
      );
    } else {
      await rejectAgreementVersion(
        negotiation.negotiationId,
        version.versionId,
      );

      toast.success("Agreement version rejected.");
    }

    await onRefresh();

  } catch (error: any) {
    toast.error(
      error?.response?.data?.message ??
        `Could not ${action} the agreement version.`,
    );

  } finally {
    setPendingActionId(null);
  }
};

  const formatDate = (value?: string | null) =>
    value ? new Date(value).toLocaleString() : "Not yet";

  return (
    <section className="space-y-4 rounded-xl border p-4">
      <div>
        <h3 className="font-semibold">
          {directBarter
            ? "Direct Barter agreement"
            : "Release method and agreement"}
        </h3>

        {!activeVersion && agreementSyncStatus !== "idle" && (
  <p className="mt-2 text-xs text-muted-foreground">
    Current Deal collaboration: {agreementSyncStatus}
  </p>
)}

        {!directBarter && (
            <p className="mt-1 text-xs text-muted-foreground">
              Each participant chooses independently.
              Both choices must match before the agreement
              can be submitted and accepted.
            </p>
          )}
      </div>

      
      {!directBarter && (

      <div className="space-y-3 rounded-lg bg-muted/30 p-3">
        <div className="space-y-1">
          <Label htmlFor="release-mode-choice">
            Your release method
          </Label>

          <select
            id="release-mode-choice"
            value={releaseMode}
            disabled={versions.length > 0 || savingChoice}
            onChange={(event) =>
              setReleaseMode(
                event.target.value as AgreementReleaseMode,
              )
            }
            className="w-full rounded-md border bg-background px-3 py-2 text-sm"
          >
            <option value="DIRECT_RELEASE">
              Direct Release — one-time release
            </option>

            <option value="MILESTONE_RELEASE">
              Milestone Release — staged funding
            </option>
          </select>
        </div>

        {releaseMode === "MILESTONE_RELEASE" && (
          <div className="space-y-1">
            <Label htmlFor="milestone-count-choice">
              Number of milestones
            </Label>

            <select
              id="milestone-count-choice"
              value={milestoneCount}
              disabled={versions.length > 0 || savingChoice}
              onChange={(event) =>
                setMilestoneCount(Number(event.target.value))
              }
              className="w-full rounded-md border bg-background px-3 py-2 text-sm"
            >
              {[2, 3, 4, 5].map((count) => (
                <option key={count} value={count}>
                  {count} milestones
                </option>
              ))}
            </select>
          </div>
        )}

        <div className="space-y-1 text-xs">
          <p>
            Requester:{" "}
            <strong>
              {negotiation.requesterReleaseMode ??
                "Not selected"}
            </strong>

            {negotiation.requesterReleaseMode ===
              "MILESTONE_RELEASE" &&
              ` (${negotiation.requesterMilestoneCount ?? "?"} milestones)`}
          </p>

          <p>
            Provider:{" "}
            <strong>
              {negotiation.providerReleaseMode ??
                "Not selected"}
            </strong>

            {negotiation.providerReleaseMode ===
              "MILESTONE_RELEASE" &&
              ` (${negotiation.providerMilestoneCount ?? "?"} milestones)`}
          </p>
        </div>

        {versions.length > 0 && (
          <p className="text-xs text-muted-foreground">
            Release-mode selection is locked because
            agreement-version history has started.
          </p>
        )}

        <Button
          className="w-full"
          onClick={handleSaveChoice}
          disabled={savingChoice || versions.length > 0}
        >
          {savingChoice ? "Saving..." : "Save my release choice"}
        </Button>

        {releaseChoiceAgreed ? (
          <p className="text-xs text-emerald-700 dark:text-emerald-400">
            Both choices match
            {agreedReleaseMode === "MILESTONE_RELEASE"
              ? `, including ${planCount} milestones`
              : ""}.
          </p>
        ) : (
          <p className="text-xs text-muted-foreground">
            Waiting for both participants to select
            matching release methods and, when applicable,
            matching milestone counts.
          </p>
        )}
      </div>

      )}

      {canCreateDraft && (
        <div className="space-y-4 border-t pt-4">
          <div>
            <h4 className="font-semibold">
              {activeVersion
                  ? `Propose amendment to version ${activeVersion.versionNumber}`
                  : "Create initial agreement version"}
            </h4>

            {directBarter ? (
                    <p className="mt-1 text-xs text-muted-foreground">
                      Direct Barter has no token escrow or token-funded milestones.
                    </p>
                  ) : (
                    <p className="mt-1 text-xs text-muted-foreground">
                      Total agreed amount:{" "}
                      {negotiation.tokenAmount ?? 0} BLT.
                      Milestone allocations must equal this
                      amount exactly.
                    </p>
                  )}
          </div>

          <div className="space-y-1">
  <Label htmlFor="agreement-deadline">
    Agreement deadline
  </Label>

  <Input
    id="agreement-deadline"
    type="datetime-local"
    value={toLocalDateTimeInput(
      agreedDeadlineField.value,
    )}
    min={localDateTimeDaysFromNow(0)}
    onChange={(event) => {
      const value = event.target.value;

      setAgreedDeadline(value);

      agreedDeadlineField.onChange(
        value ? toInstant(value) : "",
      );
    }}
  />
</div>

          <div className="space-y-1">
  <Label htmlFor="version-summary">
    Agreement summary
  </Label>

  <Input
    id="version-summary"
    value={changeSummaryField.value}
    onChange={(event) => {
      const value = event.target.value;

      setChangeSummary(value);
      changeSummaryField.onChange(value);
    }}
    maxLength={255}
  />
</div>

          <div className="space-y-1">
  <Label htmlFor="agreed-terms">
    Agreed terms and additional conditions
  </Label>

  <textarea
    id="agreed-terms"
    rows={5}
    value={agreedTermsField.value}
    onChange={(event) => {
      const value = event.target.value;

      setAgreedTerms(value);
      agreedTermsField.onChange(value);
    }}
    maxLength={10000}
    placeholder="Describe the additional terms both participants should review, such as responsibilities, deliverables, expectations, or other agreed conditions."
    className="w-full rounded-md border bg-background px-3 py-2 text-sm"
  />

  <p className="text-xs text-muted-foreground">
    Optional. These terms will be saved with this
    agreement version and remain available in its history.
  </p>
</div>

          {agreedReleaseMode === "MILESTONE_RELEASE" && (
            <div className="space-y-4">
              <div className="flex items-center justify-between gap-2">
                <h4 className="font-medium">
                  Milestone plan
                </h4>

                <span
                  className={`text-xs ${
                    allocationTotal ===
                    (negotiation.tokenAmount ?? -1)
                      ? "text-emerald-700 dark:text-emerald-400"
                      : "text-muted-foreground"
                  }`}
                >
                  {allocationTotal} /{" "}
                  {negotiation.tokenAmount ?? 0} BLT
                </span>
              </div>

             {milestones.map((milestone, index) => (
  <CollaborativeCurrentDealMilestone
    key={milestone.milestoneId ?? index}
    manager={agreementManager}
    milestone={milestone}
    index={index}
    agreedDeadline={agreedDeadline}
    onUpdate={updateMilestone}
  />
))}
            </div>
          )}

          <Button
            className="w-full"
            onClick={handleCreateDraft}
            disabled={creatingDraft}
          >
            {creatingDraft
              ? "Creating draft..."
              : "Create agreement version draft"}
          </Button>
        </div>
      )}

      {versions.length > 0 && (
        <div className="space-y-3 border-t pt-4">
          <h4 className="font-semibold">
            Agreement version history
          </h4>

          {[...versions]
            .sort(
              (a, b) =>
                b.versionNumber - a.versionNumber,
            )
            .map((version) => {
              const currentUserAccepted = isRequester
                ? !!version.requesterAcceptedAt
                : isProvider
                  ? !!version.providerAcceptedAt
                  : false;

              const isParticipant =
                isRequester || isProvider;

                  // Identify the participant who submitted this version.
  const isVersionSubmitter = version.submittedById
    ? version.submittedById === currentUserId
    : version.createdById === currentUserId;


              return (
                <div
                  key={version.versionId}
                  className="space-y-3 rounded-lg border p-3"
                >
                  <div className="flex items-start justify-between gap-2">
                    <div>
                      <p className="font-medium">
                        Version {version.versionNumber}
                      </p>

                      <p className="text-xs text-muted-foreground">
                       {version.exchangeMode === "DIRECT_BARTER"
                          ? "Direct Barter · no token escrow"
                          : version.releaseMode
                            ? `${version.releaseMode.replaceAll("_", " ")} · ${version.agreedTokenAmount} BLT`
                            : "Release mode unavailable"}
                      </p>
                    </div>

                    <span className="rounded-full bg-muted px-2 py-1 text-[11px]">
                      {statusLabels[version.status]}
                    </span>
                  </div>

                  <p className="text-xs text-muted-foreground">
                    {version.changeSummary}
                  </p>

                  {version.status === "DRAFT" && isParticipant && (
  <CollaborativeAgreementEditor
    negotiationId={negotiation.negotiationId}
    version={version}
    onSubmitted={onRefresh}
  />
)}

{version.status !== "DRAFT" &&
  version.agreedTerms?.trim() && (
    <div className="space-y-1 rounded-md bg-muted/30 p-3">
      <p className="text-xs font-medium">
        Agreed terms and additional conditions
      </p>

      <p className="whitespace-pre-wrap text-sm">
        {version.agreedTerms}
      </p>
    </div>
  )}

                  <div className="text-xs text-muted-foreground">
                    <p>
                      Requester accepted:{" "}
                      {formatDate(version.requesterAcceptedAt)}
                    </p>

                    <p>
                      Provider accepted:{" "}
                      {formatDate(version.providerAcceptedAt)}
                    </p>
                  </div>

                  {version.releaseMode ===
                    "MILESTONE_RELEASE" && (
                    <div className="space-y-2 border-t pt-2">
                      {[...version.milestones]
                        .sort(
                          (a, b) =>
                            a.milestoneNumber -
                            b.milestoneNumber,
                        )
                        .map((milestone) => (
                          <div
                            key={milestone.versionMilestoneId}
                            className="text-xs"
                          >
                            <p className="font-medium">
                              {milestone.milestoneNumber}.{" "}
                              {milestone.title} —{" "}
                              {milestone.tokenAllocation} BLT
                            </p>

                            <p className="text-muted-foreground">
                              {milestone.deliverable}
                            </p>

                            <p className="text-muted-foreground">
                              Due {formatDate(milestone.deadline)}
                              {" · "}
                              {milestone.executionStatus.replaceAll(
                                "_",
                                " ",
                              )}
                            </p>
                          </div>
                        ))}
                    </div>
                  )}

                  {version.activationFailureReason && (
                    <p className="text-xs text-destructive">
                      Funding or activation issue:{" "}
                      {version.activationFailureReason}
                    </p>
                  )}

                  {version.status === "PENDING_ACCEPTANCE" &&
  isParticipant &&
  isVersionSubmitter && (
    <p className="text-xs text-muted-foreground">
      You submitted this agreement. Your confirmation
      is recorded, and it is waiting for the other
      participant to accept.
    </p>
  )}

                  <div className="flex flex-wrap gap-2">
                  
                  {version.status === "PENDING_ACCEPTANCE" &&
  isParticipant &&
  !isVersionSubmitter &&
  !currentUserAccepted && (
                        <Button
                          size="sm"
                          onClick={() =>
                            runVersionAction(version, "accept")
                          }
                          disabled={pendingActionId !== null}
                        >
                          {pendingActionId ===
                          `${version.versionId}:accept`
                            ? "Accepting..."
                            : "Accept version"}
                        </Button>
                      )}

                    {(version.status === "DRAFT" ||
                      version.status ===
                        "PENDING_ACCEPTANCE") &&
                      isParticipant && (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() =>
                            runVersionAction(version, "reject")
                          }
                          disabled={pendingActionId !== null}
                        >
                          Reject version
                        </Button>
                      )}
                  </div>

                  {version.status ===
  "READY_FOR_ACTIVATION" && (
  <p className="text-xs text-muted-foreground">
    {version.activationFailureReason ? (
      <>
        Both participants have accepted this version,
        but activation is pending because a funding
        adjustment requires resolution. The previously
        active version remains in force.
      </>
    ) : version.versionNumber > 1 ? (
      <>
        Both participants have accepted this amendment.
        It is awaiting activation.
      </>
    ) : (
      <>
        Both participants have accepted the initial
        version. The remaining session and funding
        steps must complete before the version is active.
      </>
    )}
  </p>
)}
                </div>
              );
            })}
        </div>
      )}
    </section>
  );
}

export default AgreementVersionWorkflow;