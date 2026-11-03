import {
  useCallback,
  useEffect,
  useState,
  type FormEvent,
} from "react";

import {
  AlertCircle,
  CheckCircle2,
  ChevronDown,
  FileText,
  MessageSquare,
  RefreshCw,
  Scale,
  ShieldCheck,
  Users,
  Vote,
} from "lucide-react";

import toast from "react-hot-toast";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Textarea } from "@/components/ui/textarea";

import {
  castDisputeVote,
  getAssignedDisputes,
  getDisputeReview,
} from "@/services/DisputeService";

import type {
  AssignedDisputeSummary,
  DisputeCaseReview,
  DisputeCaseStatus,
  DisputeDecisionOutcome,
  DisputeEvidenceItem,
} from "@/models/disputes/DisputeTypes";

type EvidenceTab = "conversation" | "proofs" | "agreements";

function getErrorMessage(
  error: unknown,
  fallback: string,
): string {
  if (
    typeof error === "object" &&
    error !== null &&
    "response" in error
  ) {
    const response = (
      error as {
        response?: {
          data?: {
            message?: unknown;
          };
        };
      }
    ).response;

    if (typeof response?.data?.message === "string") {
      return response.data.message;
    }
  }

  return fallback;
}

function normalizeName(
  value: string | null | undefined,
): string {
  return value?.trim().toLocaleLowerCase() ?? "";
}

function formatDateTime(
  value: string | null | undefined,
): string {
  if (!value) {
    return "Time unavailable";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Time unavailable";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function formatTime(
  value: string | null | undefined,
): string {
  if (!value) {
    return "";
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "";
  }

  return new Intl.DateTimeFormat(undefined, {
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

function formatStatus(
  value: string | null | undefined,
): string {
  if (!value) {
    return "Unknown";
  }

  return value
    .toLowerCase()
    .split("_")
    .map(
      (part) => part.charAt(0).toUpperCase() + part.slice(1),
    )
    .join(" ");
}

function getOutcomeLabel(
  outcome: DisputeDecisionOutcome | null | undefined,
): string {
  if (outcome === "REQUESTER_FAVORED") {
    return "Requester favored";
  }

  if (outcome === "PROVIDER_FAVORED") {
    return "Provider favored";
  }

  return "Not decided";
}

function isVoteOpen(status: DisputeCaseStatus): boolean {
  return status === "EVIDENCE_OPEN" || status === "VOTING";
}

function isSafeExternalUrl(
  value: string | null | undefined,
): value is string {
  if (!value) {
    return false;
  }

  try {
    const url = new URL(value);

    return (
      (url.protocol === "https:" || url.protocol === "http:") &&
      Boolean(url.hostname) &&
      !url.username &&
      !url.password
    );
  } catch {
    return false;
  }
}

/**
 * The backend supplies both:
 * - systemGenerated: the generated-message flag
 * - sourceStatus: the original negotiation message type
 *
 * The existing session chat treats message type SYSTEM as a
 * timeline event. This component follows the same rule.
 */
function isSystemMessage(
  item: DisputeEvidenceItem,
): boolean {
  return (
    item.systemGenerated === true ||
    item.sourceStatus?.toUpperCase() === "SYSTEM" ||
    normalizeName(item.submittedByName) === "system"
  );
}

function EvidenceChatMessage({
  item,
  claimantName,
  respondentName,
}: {
  item: DisputeEvidenceItem;
  claimantName: string;
  respondentName: string;
}) {
  const createdAt = item.sourceCreatedAt ?? item.attachedAt;
  const time = formatTime(createdAt);
  const content = item.content?.trim() || item.title || "";

  /*
   * System messages are still evidence. Display them as
   * centered timeline events rather than participant bubbles.
   */
  if (isSystemMessage(item)) {
    return (
      <div className="flex items-center gap-3 py-2">
        <div className="h-px min-w-4 flex-1 bg-border" />

        <div className="max-w-[85%] text-center">
          <p className="text-xs font-medium leading-5 text-muted-foreground">
            {content || "System event"}
          </p>

          {time && (
            <p className="mt-1 text-[11px] text-muted-foreground/70">
              {time}
            </p>
          )}
        </div>

        <div className="h-px min-w-4 flex-1 bg-border" />
      </div>
    );
  }

  const senderName = item.submittedByName || "Unknown participant";
  const normalizedSender = normalizeName(senderName);

  const isClaimant =
    normalizedSender !== "" &&
    normalizedSender === normalizeName(claimantName);

  const isRespondent =
    normalizedSender !== "" &&
    normalizedSender === normalizeName(respondentName);

  /*
   * This is a read-only review for a third-party panelist.
   * The claimant is consistently left-aligned and the respondent
   * right-aligned; both names remain visible.
   */
  const alignRight = isRespondent;

  const roleLabel = isClaimant
    ? "Claimant"
    : isRespondent
      ? "Respondent"
      : null;

  return (
    <div
      className={`flex ${
        alignRight ? "justify-end" : "justify-start"
      }`}
    >
      <div
        className={`max-w-[92%] min-w-0 rounded-2xl border px-3 py-2.5 shadow-sm sm:max-w-[78%] ${
          alignRight
            ? "border-blue-600 bg-blue-600 text-white"
            : "border-border bg-card text-card-foreground"
        }`}
      >
        <div className="mb-1 flex flex-wrap items-center gap-x-2 gap-y-1">
          <p
            className={`text-xs font-semibold ${
              alignRight ? "text-blue-50" : "text-foreground"
            }`}
          >
            {senderName}
          </p>

          {roleLabel && (
            <span
              className={`text-[10px] ${
                alignRight
                  ? "text-blue-100"
                  : "text-muted-foreground"
              }`}
            >
              {roleLabel}
            </span>
          )}
        </div>

        <p className="whitespace-pre-wrap break-words text-sm leading-6">
          {content || "Message content unavailable."}
        </p>

        {isSafeExternalUrl(item.attachmentUrl) && (
          <a
            href={item.attachmentUrl}
            target="_blank"
            rel="noopener noreferrer"
            className={`mt-2 inline-block break-all text-xs underline underline-offset-4 ${
              alignRight ? "text-blue-100" : "text-primary"
            }`}
          >
            Open message attachment
          </a>
        )}

        {time && (
          <p
            className={`mt-1.5 text-right text-[10px] ${
              alignRight
                ? "text-blue-100"
                : "text-muted-foreground"
            }`}
          >
            {time}
          </p>
        )}
      </div>
    </div>
  );
}

function EvidenceConversation({
  evidence,
  claimantName,
  respondentName,
}: {
  evidence: DisputeEvidenceItem[];
  claimantName: string;
  respondentName: string;
}) {
  const [isExpanded, setIsExpanded] = useState(true);

  /*
   * Preserve the original message order using source timestamps.
   */
  const messages = [...evidence]
    .filter(
      (item) => item.evidenceType === "NEGOTIATION_MESSAGE",
    )
    .sort((a, b) => {
      const timeA = a.sourceCreatedAt ?? a.attachedAt;
      const timeB = b.sourceCreatedAt ?? b.attachedAt;

      const dateA = timeA ? new Date(timeA).getTime() : 0;
      const dateB = timeB ? new Date(timeB).getTime() : 0;

      return dateA - dateB;
    });

  if (messages.length === 0) {
    return (
      <div className="flex min-h-40 items-center justify-center rounded-lg border border-dashed p-6 text-center">
        <div>
          <MessageSquare
            className="mx-auto text-muted-foreground"
            size={25}
          />

          <p className="mt-2 text-sm font-medium">
            No negotiation messages attached
          </p>

          <p className="mt-1 text-xs text-muted-foreground">
            Conversation evidence will appear here when available.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-xl border">
      {/* Always-visible, clickable conversation header */}
      <button
        type="button"
        aria-expanded={isExpanded}
        aria-controls="dispute-conversation-content"
        onClick={() => setIsExpanded((current) => !current)}
        className="flex w-full items-center justify-between gap-3 border-b px-4 py-3 text-left transition-colors hover:bg-muted/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-inset"
      >
        <div className="flex min-w-0 items-center gap-2">
          <MessageSquare
            size={17}
            className="shrink-0 text-primary"
          />

          <span className="truncate text-sm font-semibold">
            Conversation history
          </span>

          <span className="shrink-0 rounded-full bg-muted px-2 py-0.5 text-[11px] text-muted-foreground">
            {messages.length}
          </span>
        </div>

        <div className="flex shrink-0 items-center gap-2">
          <span className="hidden text-xs text-muted-foreground sm:inline">
            {isExpanded ? "Collapse" : "Expand"}
          </span>

          <ChevronDown
            size={17}
            className={`text-muted-foreground transition-transform duration-200 ${
              isExpanded ? "rotate-180" : ""
            }`}
          />
        </div>
      </button>

      {/* Messages and footer collapse together */}
      {isExpanded && (
        <div id="dispute-conversation-content">
          <div className="max-h-[440px] space-y-3 overflow-y-auto bg-muted/20 p-3 sm:p-4">
            {messages.map((item) => (
              <EvidenceChatMessage
                key={item.evidenceReferenceId}
                item={item}
                claimantName={claimantName}
                respondentName={respondentName}
              />
            ))}
          </div>

          <div className="border-t px-4 py-2">
            <p className="text-[11px] text-muted-foreground">
              Read-only conversation evidence. Messages are shown
              in their original chronological order.
            </p>
          </div>
        </div>
      )}
    </div>
  );
}

function SessionProofs({
  evidence,
}: {
  evidence: DisputeEvidenceItem[];
}) {
  const proofs = evidence.filter(
    (item) => item.evidenceType === "SESSION_PROOF",
  );

  if (proofs.length === 0) {
    return (
      <div className="rounded-lg border border-dashed p-6 text-center">
        <FileText
          className="mx-auto text-muted-foreground"
          size={25}
        />

        <p className="mt-2 text-sm font-medium">
          No session proofs attached
        </p>

        <p className="mt-1 text-xs text-muted-foreground">
          The backend did not return any session-proof references.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {proofs.map((item) => {
        const externalUrl = isSafeExternalUrl(item.proofUrl)
          ? item.proofUrl
          : null;

        return (
          <article
            key={item.evidenceReferenceId}
            className="rounded-lg border p-3"
          >
            <div className="flex flex-wrap items-start justify-between gap-2">
              <div className="min-w-0">
                <h3 className="break-words text-sm font-semibold">
                  {item.title || item.fileName || "Session proof"}
                </h3>

                <p className="mt-1 text-xs text-muted-foreground">
                  {item.submittedByName
                    ? `Submitted by ${item.submittedByName}`
                    : "Uploader unavailable"}
                  {" · "}
                  {formatDateTime(
                    item.sourceCreatedAt ?? item.attachedAt,
                  )}
                </p>
              </div>

              <Badge variant="outline">
                {formatStatus(item.proofType)}
              </Badge>
            </div>

            {item.content &&
              item.content !== item.title && (
                <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-5 text-muted-foreground">
                  {item.content}
                </p>
              )}

            {item.fileName && (
              <div className="mt-3 rounded-md bg-muted/40 p-2.5">
                <p className="break-words text-sm font-medium">
                  {item.fileName}
                </p>

                <p className="mt-1 text-xs text-muted-foreground">
                  {item.contentType || "File"}
                  {item.fileSize != null
                    ? ` · ${(item.fileSize / (1024 * 1024)).toFixed(2)} MB`
                    : ""}
                </p>
              </div>
            )}

            {externalUrl && (
              <a
                href={externalUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-3 inline-block break-all text-sm text-primary underline underline-offset-4"
              >
                Open proof link
              </a>
            )}

            {item.sha256 && (
              <details className="mt-3">
                <summary className="cursor-pointer text-xs text-muted-foreground">
                  View integrity hash
                </summary>

                <p className="mt-2 break-all font-mono text-[11px] text-muted-foreground">
                  {item.sha256}
                </p>
              </details>
            )}

            {item.fileName && !externalUrl && (
              <p className="mt-2 text-xs text-muted-foreground">
                File metadata is available. A case-authorized
                panelist download must be supported by the backend
                before the original file can be opened here.
              </p>
            )}
          </article>
        );
      })}
    </div>
  );
}

function AgreementVersions({
  evidence,
}: {
  evidence: DisputeEvidenceItem[];
}) {
  const versions = evidence
    .filter((item) => item.evidenceType === "AGREEMENT_VERSION")
    .sort(
      (a, b) =>
        (b.versionNumber ?? 0) - (a.versionNumber ?? 0),
    );

  if (versions.length === 0) {
    return (
      <div className="rounded-lg border border-dashed p-6 text-center">
        <FileText
          className="mx-auto text-muted-foreground"
          size={25}
        />

        <p className="mt-2 text-sm font-medium">
          No agreement versions attached
        </p>

        <p className="mt-1 text-xs text-muted-foreground">
          The backend did not return agreement-version references.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {versions.map((item) => (
        <details
          key={item.evidenceReferenceId}
          className="group rounded-lg border"
        >
          <summary className="flex cursor-pointer list-none items-center justify-between gap-3 p-3">
            <div className="min-w-0">
              <p className="text-sm font-semibold">
                {item.title ||
                  `Agreement version ${item.versionNumber ?? ""}`}
              </p>

              <p className="mt-1 text-xs text-muted-foreground">
                {item.submittedByName
                  ? `Created by ${item.submittedByName}`
                  : "Creator unavailable"}
                {" · "}
                {formatDateTime(
                  item.sourceCreatedAt ?? item.attachedAt,
                )}
              </p>
            </div>

            <Badge variant="outline">
              {formatStatus(item.sourceStatus)}
            </Badge>
          </summary>

          <div className="border-t bg-muted/10 p-3">
            <p className="whitespace-pre-wrap break-words text-sm leading-6">
              {item.content || "No version details available."}
            </p>
          </div>
        </details>
      ))}
    </div>
  );
}

function EvidencePanel({
  evidence,
  claimantName,
  respondentName,
}: {
  evidence: DisputeEvidenceItem[];
  claimantName: string;
  respondentName: string;
}) {
  const [activeTab, setActiveTab] =
    useState<EvidenceTab>("conversation");

  const conversationCount = evidence.filter(
    (item) => item.evidenceType === "NEGOTIATION_MESSAGE",
  ).length;

  const proofCount = evidence.filter(
    (item) => item.evidenceType === "SESSION_PROOF",
  ).length;

  const agreementCount = evidence.filter(
    (item) => item.evidenceType === "AGREEMENT_VERSION",
  ).length;

  const tabs: {
    id: EvidenceTab;
    label: string;
    count: number;
  }[] = [
    {
      id: "conversation",
      label: "Conversation",
      count: conversationCount,
    },
    {
      id: "proofs",
      label: "Session Proofs",
      count: proofCount,
    },
    {
      id: "agreements",
      label: "Agreement Versions",
      count: agreementCount,
    },
  ];

  return (
    <Card className="min-w-0">
      <CardHeader className="space-y-1 p-4 pb-3">
        <div className="flex items-center justify-between gap-3">
          <CardTitle className="flex items-center gap-2 text-base">
            <ShieldCheck size={19} className="text-primary" />
            Evidence
          </CardTitle>

          <span className="text-xs text-muted-foreground">
            {evidence.length} references
          </span>
        </div>

        <p className="text-xs leading-5 text-muted-foreground">
          Review the conversation, uploaded proofs, and agreement
          history associated with this case.
        </p>
      </CardHeader>

      <CardContent className="space-y-3 p-4 pt-0">
        <div
          role="tablist"
          aria-label="Dispute evidence type"
          className="flex flex-wrap gap-2 border-b pb-3"
        >
          {tabs.map((tab) => {
            const selected = activeTab === tab.id;

            return (
              <button
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={selected}
                onClick={() => setActiveTab(tab.id)}
                className={`rounded-lg border px-3 py-2 text-xs font-medium transition-colors ${
                  selected
                    ? "border-primary bg-primary text-primary-foreground"
                    : "border-border bg-background text-muted-foreground hover:bg-muted hover:text-foreground"
                }`}
              >
                {tab.label}
                <span className="ml-1.5 opacity-80">
                  {tab.count}
                </span>
              </button>
            );
          })}
        </div>

        {activeTab === "conversation" && (
          <EvidenceConversation
            evidence={evidence}
            claimantName={claimantName}
            respondentName={respondentName}
          />
        )}

        {activeTab === "proofs" && (
          <SessionProofs evidence={evidence} />
        )}

        {activeTab === "agreements" && (
          <AgreementVersions evidence={evidence} />
        )}
      </CardContent>
    </Card>
  );
}

export default function GovernancePage() {
  const [assignments, setAssignments] = useState<
    AssignedDisputeSummary[]
  >([]);

  const [selectedDisputeId, setSelectedDisputeId] =
    useState<string | null>(null);

    const [isReviewExpanded, setIsReviewExpanded] = useState(true);

    const [expandedDisputeId, setExpandedDisputeId] =
    useState<string | null>(null);

  const [review, setReview] =
    useState<DisputeCaseReview | null>(null);

  const [loadingAssignments, setLoadingAssignments] =
    useState(true);

  const [loadingReview, setLoadingReview] =
    useState(false);

  const [pageError, setPageError] =
    useState<string | null>(null);

  const [reviewError, setReviewError] =
    useState<string | null>(null);

  const [refreshing, setRefreshing] =
    useState(false);

  const [reviewReloadCounter, setReviewReloadCounter] =
    useState(0);

  const [selectedOutcome, setSelectedOutcome] =
    useState<DisputeDecisionOutcome>(
      "REQUESTER_FAVORED",
    );

  const [voteReason, setVoteReason] = useState("");

  const [submittingVote, setSubmittingVote] =
    useState(false);

  const loadAssignments = useCallback(async () => {
    setLoadingAssignments(true);
    setPageError(null);

    try {
      const result = await getAssignedDisputes();

      setAssignments(result);

      setSelectedDisputeId((current) => {
        if (
          current &&
          result.some(
            (item) => item.disputeId === current,
          )
        ) {
          return current;
        }

        return result[0]?.disputeId ?? null;
      });
    } catch (error: unknown) {
      setPageError(
        getErrorMessage(
          error,
          "Unable to load your assigned disputes.",
        ),
      );

      setAssignments([]);
      setSelectedDisputeId(null);
    } finally {
      setLoadingAssignments(false);
    }
  }, []);

  useEffect(() => {
    void loadAssignments();
  }, [loadAssignments]);

  useEffect(() => {
    if (!selectedDisputeId) {
      setReview(null);
      setReviewError(null);
      setLoadingReview(false);
      return;
    }

    let active = true;

    setLoadingReview(true);
    setReviewError(null);
    setReview(null);
    setVoteReason("");

    getDisputeReview(selectedDisputeId)
      .then((result) => {
        if (active) {
          setReview(result);
        }
      })
      .catch((error: unknown) => {
        if (active) {
          setReviewError(
            getErrorMessage(
              error,
              "Unable to load this dispute review.",
            ),
          );
        }
      })
      .finally(() => {
        if (active) {
          setLoadingReview(false);
        }
      });

    return () => {
      active = false;
    };
  }, [selectedDisputeId, reviewReloadCounter]);

  const selectedAssignment = assignments.find(
    (item) => item.disputeId === selectedDisputeId,
  );

  const voteOpenCount = assignments.filter(
    (item) => isVoteOpen(item.caseStatus),
  ).length;

  const decidedCount = assignments.filter(
    (item) => Boolean(item.decisionOutcome),
  ).length;

  const canVote = Boolean(
    review &&
      isVoteOpen(review.status) &&
      !review.currentUserHasVoted &&
      !review.decisionOutcome,
  );

  const handleRefresh = async () => {
    setRefreshing(true);

    try {
      await loadAssignments();

      if (selectedDisputeId) {
        setReviewReloadCounter((current) => current + 1);
      }
    } finally {
      setRefreshing(false);
    }
  };

  const handleSubmitVote = async (
    event: FormEvent<HTMLFormElement>,
  ) => {
    event.preventDefault();

    if (
      !review ||
      !canVote ||
      !voteReason.trim() ||
      submittingVote
    ) {
      return;
    }

    try {
      setSubmittingVote(true);

      const result = await castDisputeVote(
        review.disputeId,
        {
          outcome: selectedOutcome,
          reason: voteReason.trim(),
        },
      );

      if (result.decisionOutcome) {
        toast.success(
          "Your vote was recorded and the backend declared a final decision.",
        );
      } else {
        toast.success(
          `Your vote was recorded for evidence version ${result.evidenceVersion}.`,
        );
      }

      setVoteReason("");

      const refreshedReview = await getDisputeReview(
        review.disputeId,
      );

      setReview(refreshedReview);

      await loadAssignments();
    } catch (error: unknown) {
      toast.error(
        getErrorMessage(
          error,
          "Unable to submit your vote.",
        ),
      );
    } finally {
      setSubmittingVote(false);
    }
  };

  return (
    <div className="w-full min-w-0 space-y-4">
      {/* Compact page header */}
      <header className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <div className="min-w-0">
          <div className="flex items-center gap-2.5">
            <Scale
              className="shrink-0 text-primary"
              size={25}
            />

            <h1 className="text-2xl font-bold tracking-tight">
              Dispute Governance
            </h1>
          </div>

          <p className="mt-1.5 max-w-3xl text-sm leading-5 text-muted-foreground">
            Review assigned cases, inspect the evidence, and
            submit a reasoned vote.
          </p>
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => void handleRefresh()}
          disabled={refreshing || loadingAssignments}
          className="shrink-0"
        >
          <RefreshCw
            size={15}
            className={refreshing ? "animate-spin" : ""}
          />
          Refresh
        </Button>
      </header>

      {/* Compact statistics row */}
      <div className="grid grid-cols-3 gap-2.5">
        <div className="flex min-w-0 items-center gap-2.5 rounded-xl border bg-card px-3 py-2.5">
          <Users
            size={18}
            className="shrink-0 text-primary"
          />

          <div className="min-w-0">
            <p className="truncate text-xs text-muted-foreground">
              Assigned cases
            </p>

            <p className="text-lg font-semibold leading-6">
              {assignments.length}
            </p>
          </div>
        </div>

        <div className="flex min-w-0 items-center gap-2.5 rounded-xl border bg-card px-3 py-2.5">
          <Vote
            size={18}
            className="shrink-0 text-primary"
          />

          <div className="min-w-0">
            <p className="truncate text-xs text-muted-foreground">
              Voting open
            </p>

            <p className="text-lg font-semibold leading-6">
              {voteOpenCount}
            </p>
          </div>
        </div>

        <div className="flex min-w-0 items-center gap-2.5 rounded-xl border bg-card px-3 py-2.5">
          <CheckCircle2
            size={18}
            className="shrink-0 text-primary"
          />

          <div className="min-w-0">
            <p className="truncate text-xs text-muted-foreground">
              Decisions
            </p>

            <p className="text-lg font-semibold leading-6">
              {decidedCount}
            </p>
          </div>
        </div>
      </div>

      {/* Main case list and review */}
      <div className="grid min-w-0 grid-cols-1 items-start gap-4 xl:grid-cols-[260px_minmax(0,1fr)]">
        {/* Assigned cases */}
        <Card className="min-w-0 xl:sticky xl:top-4">
          <CardHeader className="p-4 pb-3">
            <CardTitle className="flex items-center gap-2 text-base">
              <Users size={18} />
              Assigned to me
            </CardTitle>
          </CardHeader>

          <CardContent className="space-y-2 p-3 pt-0">
            {loadingAssignments && (
              <p className="py-6 text-center text-sm text-muted-foreground">
                Loading assigned cases…
              </p>
            )}

            {!loadingAssignments && pageError && (
              <div className="rounded-lg border border-destructive/40 p-3">
                <p className="flex items-center gap-2 text-sm font-medium text-destructive">
                  <AlertCircle size={16} />
                  Could not load cases
                </p>

                <p className="mt-2 break-words text-xs leading-5">
                  {pageError}
                </p>

                <Button
                  className="mt-3 w-full"
                  variant="outline"
                  size="sm"
                  onClick={() => void loadAssignments()}
                >
                  Try again
                </Button>
              </div>
            )}

            {!loadingAssignments &&
              !pageError &&
              assignments.length === 0 && (
                <div className="py-7 text-center">
                  <Users
                    className="mx-auto text-muted-foreground"
                    size={25}
                  />

                  <p className="mt-2 text-sm font-medium">
                    No assigned cases
                  </p>

                  <p className="mt-1 text-xs leading-5 text-muted-foreground">
                    Cases assigned to your account will appear here.
                  </p>
                </div>
              )}

            {!loadingAssignments &&
              !pageError &&
              assignments.map((assignment) => {
  const selected =
    selectedDisputeId === assignment.disputeId;

  const expanded =
    expandedDisputeId === assignment.disputeId;

  return (
    <article
      key={assignment.disputeId}
      className={`min-w-0 overflow-hidden rounded-lg border transition-colors ${
        selected
          ? "border-primary bg-primary/5"
          : "border-border"
      }`}
    >
      <button
        type="button"
        aria-expanded={expanded}
        aria-controls={`dispute-details-${assignment.disputeId}`}
        onClick={() => {
  if (selectedDisputeId !== assignment.disputeId) {
    setIsReviewExpanded(true);
  }

  setSelectedDisputeId(assignment.disputeId);

  setExpandedDisputeId((current) =>
    current === assignment.disputeId
      ? null
      : assignment.disputeId,
  );
}}
        className="w-full min-w-0 p-3 text-left hover:bg-muted/40 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-inset"
      >
        <div className="flex items-start gap-2">
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <p className="text-sm font-semibold">
                Dispute case
              </p>

              <Badge
                variant={
                  isVoteOpen(assignment.caseStatus)
                    ? "default"
                    : "outline"
                }
                className="text-[10px]"
              >
                {formatStatus(assignment.caseStatus)}
              </Badge>
            </div>

            <p className="mt-1.5 break-all font-mono text-[10px] leading-4 text-muted-foreground">
              {assignment.disputeId}
            </p>

            {!expanded && (
              <p className="mt-2 line-clamp-2 break-words text-xs leading-5 text-muted-foreground">
                {assignment.reason}
              </p>
            )}
          </div>

          <ChevronDown
            size={17}
            className={`mt-1 shrink-0 text-muted-foreground transition-transform duration-200 ${
              expanded ? "rotate-180" : ""
            }`}
          />
        </div>

        <div className="mt-2 flex flex-wrap items-center gap-2 text-[10px] text-muted-foreground">
          <span>
            Evidence v{assignment.evidenceVersion ?? 0}
          </span>

          <span aria-hidden="true">·</span>

          <span>
            Seat {assignment.panelSeatNumber}
          </span>
        </div>
      </button>

      {expanded && (
        <div
          id={`dispute-details-${assignment.disputeId}`}
          className="space-y-3 border-t px-3 py-3"
        >
          <div>
            <p className="text-[11px] font-semibold text-muted-foreground">
              Dispute reason
            </p>

            <p className="mt-1 whitespace-pre-wrap break-words text-xs leading-5">
              {assignment.reason}
            </p>
          </div>

          <div className="grid grid-cols-2 gap-2">
            <div className="rounded-md bg-muted/40 p-2">
              <p className="text-[10px] text-muted-foreground">
                Your panel seat
              </p>

              <p className="mt-1 text-xs font-medium">
                {assignment.panelSeatNumber}
              </p>
            </div>

            <div className="rounded-md bg-muted/40 p-2">
              <p className="text-[10px] text-muted-foreground">
                Assignment
              </p>

              <p className="mt-1 break-words text-xs font-medium">
                {formatStatus(assignment.assignmentStatus)}
              </p>
            </div>

            <div className="rounded-md bg-muted/40 p-2">
              <p className="text-[10px] text-muted-foreground">
                Evidence revision
              </p>

              <p className="mt-1 text-xs font-medium">
                v{assignment.evidenceVersion ?? 0}
              </p>
            </div>

            <div className="rounded-md bg-muted/40 p-2">
              <p className="text-[10px] text-muted-foreground">
                Created
              </p>

              <p className="mt-1 text-xs font-medium">
                {formatDateTime(assignment.createdAt)}
              </p>
            </div>
          </div>

          {assignment.decisionOutcome && (
            <div className="rounded-md border p-2.5">
              <p className="text-[10px] text-muted-foreground">
                Final decision
              </p>

              <p className="mt-1 text-xs font-semibold">
                {getOutcomeLabel(assignment.decisionOutcome)}
              </p>

              {assignment.decisionSummary && (
                <p className="mt-1 whitespace-pre-wrap break-words text-xs leading-5 text-muted-foreground">
                  {assignment.decisionSummary}
                </p>
              )}
            </div>
          )}

          <p className="text-[10px] leading-4 text-muted-foreground">
            Click this case header to collapse its details.
          </p>
        </div>
      )}
    </article>
  );
})}
          </CardContent>
        </Card>

        {/* Selected case */}
<div className="min-w-0 space-y-3">
  {/* Always-visible selected-case header */}
  <div className="flex min-w-0 items-center justify-between gap-3 rounded-xl border bg-card p-3 sm:p-4">
    <div className="min-w-0 flex-1">
      <div className="flex flex-wrap items-center gap-2">
        <ShieldCheck
          size={18}
          className="shrink-0 text-primary"
        />

        <h2 className="text-sm font-semibold">
          {review ? "Case review" : "Selected dispute"}
        </h2>

        {review && (
          <Badge
            variant={
              review.decisionOutcome
                ? "secondary"
                : isVoteOpen(review.status)
                  ? "default"
                  : "outline"
            }
          >
            {formatStatus(review.status)}
          </Badge>
        )}
      </div>

      <p className="mt-1 break-all font-mono text-xs text-muted-foreground">
        {review?.disputeId ??
          selectedAssignment?.disputeId ??
          "Select an assigned dispute"}
      </p>

      {review && (
        <p className="mt-1 line-clamp-2 break-words text-xs leading-5 text-muted-foreground">
          {review.reason}
        </p>
      )}

      {review && (
        <p className="mt-1 text-[11px] text-muted-foreground">
          Evidence revision: v{review.evidenceVersion ?? 0}
          {" · "}
          Panel seat: {review.panelSeatNumber}
        </p>
      )}
    </div>

    <Button
      type="button"
      variant="outline"
      size="sm"
      disabled={!selectedDisputeId}
      aria-expanded={isReviewExpanded}
      aria-controls="governance-selected-case-content"
      onClick={() =>
        setIsReviewExpanded((current) => !current)
      }
      className="shrink-0"
    >
      <ChevronDown
        size={16}
        className={`transition-transform duration-200 ${
          isReviewExpanded ? "rotate-180" : ""
        }`}
      />

      {isReviewExpanded ? "Collapse" : "Expand"}
    </Button>
  </div>

  {/* All case details collapse together */}
  <div
    id="governance-selected-case-content"
    className={
      isReviewExpanded
        ? "min-w-0 space-y-4"
        : "hidden"
    }
  >
          {loadingReview && (
            <Card>
              <CardContent className="p-8 text-center">
                <RefreshCw
                  className="mx-auto animate-spin text-muted-foreground"
                  size={22}
                />

                <p className="mt-3 text-sm text-muted-foreground">
                  Loading case review…
                </p>
              </CardContent>
            </Card>
          )}

          {!loadingReview && reviewError && (
            <Card>
              <CardContent className="p-4">
                <p className="font-semibold text-destructive">
                  Unable to load case review
                </p>

                <p className="mt-2 break-words text-sm leading-5">
                  {reviewError}
                </p>

                {selectedDisputeId && (
                  <Button
                    className="mt-3"
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      setReviewReloadCounter(
                        (current) => current + 1,
                      )
                    }
                  >
                    Retry review
                  </Button>
                )}
              </CardContent>
            </Card>
          )}

          {!loadingReview &&
            !reviewError &&
            review && (
              <>
                {/* Compact case details */}
                <Card className="min-w-0">
                  <CardHeader className="p-4 pb-3">
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div className="min-w-0">
                        <CardTitle className="text-base">
                          Case review
                        </CardTitle>

                        <p className="mt-1.5 break-all font-mono text-xs text-muted-foreground">
                          {review.disputeId}
                        </p>
                      </div>

                      <Badge
                        variant={
                          review.decisionOutcome
                            ? "secondary"
                            : isVoteOpen(review.status)
                              ? "default"
                              : "outline"
                        }
                      >
                        {formatStatus(review.status)}
                      </Badge>
                    </div>
                  </CardHeader>

                  <CardContent className="space-y-3 p-4 pt-0">
                    <div>
                      <p className="text-xs font-medium text-muted-foreground">
                        Dispute reason
                      </p>

                      <p className="mt-1 whitespace-pre-wrap break-words text-sm leading-5">
                        {review.reason}
                      </p>
                    </div>

                    <div className="grid gap-2 sm:grid-cols-2">
                      <div className="min-w-0 rounded-lg border p-3">
                        <p className="text-[11px] text-muted-foreground">
                          Claimant
                        </p>

                        <p className="mt-1 break-words text-sm font-medium">
                          {review.claimantName}
                        </p>
                      </div>

                      <div className="min-w-0 rounded-lg border p-3">
                        <p className="text-[11px] text-muted-foreground">
                          Respondent
                        </p>

                        <p className="mt-1 break-words text-sm font-medium">
                          {review.respondentName}
                        </p>
                      </div>
                    </div>

                    <div className="flex flex-wrap gap-x-4 gap-y-1 border-t pt-3 text-xs text-muted-foreground">
                      <span>
                        Panel seat: {review.panelSeatNumber}
                      </span>

                      <span>
                        Evidence revision: v
                        {review.evidenceVersion ?? 0}
                      </span>

                      <span>
                        Opened: {formatDateTime(review.createdAt)}
                      </span>
                    </div>
                  </CardContent>
                </Card>

                {/* Grouped evidence instead of a long stack of cards */}
                <EvidencePanel
                  evidence={review.evidence ?? []}
                  claimantName={review.claimantName}
                  respondentName={review.respondentName}
                />

                {/* Final decision */}
                {review.decisionOutcome && (
                  <Card>
                    <CardHeader className="p-4 pb-2">
                      <CardTitle className="flex items-center gap-2 text-base">
                        <CheckCircle2
                          className="text-primary"
                          size={18}
                        />
                        Final panel decision
                      </CardTitle>
                    </CardHeader>

                    <CardContent className="space-y-3 p-4 pt-0">
                      <div className="rounded-lg border p-3">
                        <p className="text-xs text-muted-foreground">
                          Outcome
                        </p>

                        <p className="mt-1 text-base font-semibold">
                          {getOutcomeLabel(
                            review.decisionOutcome,
                          )}
                        </p>

                        <p className="mt-1 text-xs text-muted-foreground">
                          Declared{" "}
                          {formatDateTime(
                            review.decisionDeclaredAt,
                          )}
                        </p>
                      </div>

                      {review.decisionSummary && (
                        <p className="whitespace-pre-wrap break-words text-sm leading-5">
                          {review.decisionSummary}
                        </p>
                      )}

                      <p className="text-xs text-muted-foreground">
                        This decision is read-only. No appeal action
                        is provided by the current workflow.
                      </p>
                    </CardContent>
                  </Card>
                )}

                {/* Previously submitted vote */}
                {!review.decisionOutcome &&
                  review.currentUserHasVoted && (
                    <Card>
                      <CardContent className="space-y-2 p-4">
                        <p className="flex items-center gap-2 text-sm font-semibold">
                          <CheckCircle2
                            className="text-primary"
                            size={17}
                          />
                          Your vote is recorded
                        </p>

                        <p className="text-sm">
                          Outcome:{" "}
                          {getOutcomeLabel(
                            review.currentUserVoteOutcome,
                          )}
                        </p>

                        {review.currentUserVoteReason && (
                          <p className="whitespace-pre-wrap break-words text-sm leading-5 text-muted-foreground">
                            {review.currentUserVoteReason}
                          </p>
                        )}

                        <p className="text-xs text-muted-foreground">
                          Submitted{" "}
                          {formatDateTime(
                            review.currentUserVotedAt,
                          )}
                        </p>

                        <p className="text-xs text-muted-foreground">
                          Your vote is recorded for the current
                          evidence revision.
                        </p>
                      </CardContent>
                    </Card>
                  )}

                {/* Voting */}
                {!review.decisionOutcome &&
                  !review.currentUserHasVoted &&
                  canVote && (
                    <Card>
                      <CardHeader className="p-4 pb-3">
                        <CardTitle className="flex items-center gap-2 text-base">
                          <Vote
                            size={18}
                            className="text-primary"
                          />
                          Submit your vote
                        </CardTitle>

                        <p className="text-xs leading-5 text-muted-foreground">
                          Review the evidence above, select an outcome,
                          and explain your reasoning. Your vote applies
                          to evidence revision v
                          {review.evidenceVersion ?? 0}.
                        </p>
                      </CardHeader>

                      <CardContent className="p-4 pt-0">
                        <form
                          onSubmit={handleSubmitVote}
                          className="space-y-3"
                        >
                          <fieldset
                            disabled={submittingVote}
                            className="space-y-2"
                          >
                            <legend className="text-xs font-semibold">
                              Decision outcome
                            </legend>

                            <div className="grid gap-2 sm:grid-cols-2">
                              <label
                                className={`flex cursor-pointer items-start gap-2.5 rounded-lg border p-3 ${
                                  selectedOutcome ===
                                  "REQUESTER_FAVORED"
                                    ? "border-primary bg-primary/5"
                                    : "hover:bg-muted/50"
                                }`}
                              >
                                <input
                                  type="radio"
                                  name="disputeOutcome"
                                  className="mt-0.5"
                                  checked={
                                    selectedOutcome ===
                                    "REQUESTER_FAVORED"
                                  }
                                  onChange={() =>
                                    setSelectedOutcome(
                                      "REQUESTER_FAVORED",
                                    )
                                  }
                                />

                                <span>
                                  <span className="block text-sm font-semibold">
                                    Requester favored
                                  </span>

                                  <span className="mt-1 block text-xs leading-4 text-muted-foreground">
                                    The evidence supports the requester.
                                  </span>
                                </span>
                              </label>

                              <label
                                className={`flex cursor-pointer items-start gap-2.5 rounded-lg border p-3 ${
                                  selectedOutcome ===
                                  "PROVIDER_FAVORED"
                                    ? "border-primary bg-primary/5"
                                    : "hover:bg-muted/50"
                                }`}
                              >
                                <input
                                  type="radio"
                                  name="disputeOutcome"
                                  className="mt-0.5"
                                  checked={
                                    selectedOutcome ===
                                    "PROVIDER_FAVORED"
                                  }
                                  onChange={() =>
                                    setSelectedOutcome(
                                      "PROVIDER_FAVORED",
                                    )
                                  }
                                />

                                <span>
                                  <span className="block text-sm font-semibold">
                                    Provider favored
                                  </span>

                                  <span className="mt-1 block text-xs leading-4 text-muted-foreground">
                                    The evidence supports the provider.
                                  </span>
                                </span>
                              </label>
                            </div>
                          </fieldset>

                          <div className="space-y-1.5">
                            <label
                              htmlFor="dispute-vote-reason"
                              className="text-xs font-semibold"
                            >
                              Reason for your vote
                              <span className="ml-1 text-destructive">
                                *
                              </span>
                            </label>

                            <Textarea
                              id="dispute-vote-reason"
                              value={voteReason}
                              onChange={(event) =>
                                setVoteReason(
                                  event.target.value.slice(0, 5000),
                                )
                              }
                              rows={4}
                              maxLength={5000}
                              placeholder="Explain how the available evidence supports your decision."
                              required
                              disabled={submittingVote}
                              className="resize-y"
                            />

                            <p className="text-right text-[11px] text-muted-foreground">
                              {voteReason.length}/5000
                            </p>
                          </div>

                          <p className="rounded-lg border border-amber-500/40 bg-amber-500/5 p-3 text-xs leading-5">
                            The backend enforces panel assignment,
                            case state, evidence revision, and vote
                            eligibility.
                          </p>

                          <div className="flex justify-end">
                            <Button
                              type="submit"
                              size="sm"
                              disabled={
                                !voteReason.trim() ||
                                submittingVote
                              }
                            >
                              {submittingVote
                                ? "Submitting…"
                                : "Submit vote"}
                            </Button>
                          </div>
                        </form>
                      </CardContent>
                    </Card>
                  )}

                {/* Other non-voting states */}
                {!review.decisionOutcome &&
                  !review.currentUserHasVoted &&
                  !canVote && (
                    <Card>
                      <CardContent className="flex items-start gap-3 p-4">
                        <ShieldCheck
                          className="mt-0.5 shrink-0 text-muted-foreground"
                          size={19}
                        />

                        <div>
                          <p className="text-sm font-semibold">
                            Voting is not available
                          </p>

                          <p className="mt-1 text-xs leading-5 text-muted-foreground">
                            This case is not currently accepting
                            votes, or this panel assignment is not
                            eligible to vote. The backend enforces
                            the relevant rules.
                          </p>
                        </div>
                      </CardContent>
                    </Card>
                  )}
              </>
            )}

          {!loadingReview &&
            !reviewError &&
            !review &&
            !selectedAssignment &&
            !loadingAssignments &&
            !pageError && (
              <Card>
                <CardContent className="flex min-h-48 flex-col items-center justify-center p-6 text-center">
                  <ShieldCheck
                    className="text-muted-foreground"
                    size={29}
                  />

                  <h2 className="mt-3 text-sm font-semibold">
                    Select an assigned case
                  </h2>

                  <p className="mt-1 max-w-sm text-xs leading-5 text-muted-foreground">
                    Case details, the conversation, and evidence
                    will appear here.
                  </p>
                </CardContent>
              </Card>
            )}
          </div>
        </div>
      </div>

      <p className="text-[11px] leading-5 text-muted-foreground">
        Panel decisions and votes are managed by the backend.
        This page displays case evidence read-only.
      </p>
    </div>
  );
}