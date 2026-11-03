import { useCallback, useEffect, useState } from "react";

import {
  AlertCircle,
  CheckCircle2,
  ChevronDown,
  Circle,
  Clock3,
  Coins,
  ExternalLink,
  FileText,
  RefreshCw,
  Scale,
  ShieldCheck,
  Users,
  Vote,
} from "lucide-react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";

import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

import { getMyDisputes } from "@/services/DisputeService";

import type {
  AnonymousPanelVoteRationale,
  DisputeCaseStatus,
  DisputeDecisionOutcome,
  ParticipantDisputeSummary,
  ParticipantSettlementStatus,
} from "@/models/disputes/DisputeTypes";

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

function formatStatus(
  value: string | null | undefined,
): string {
  if (!value) return "Unknown";

  return value
    .toLowerCase()
    .split("_")
    .map(
      (part) => part.charAt(0).toUpperCase() + part.slice(1),
    )
    .join(" ");
}

function formatDate(
  value: string | null | undefined,
): string {
  if (!value) return "Not available";

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Not available";
  }

  return new Intl.DateTimeFormat(undefined, {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function isDecisionDeclared(
  item: ParticipantDisputeSummary,
): boolean {
  return item.decisionOutcome != null;
}

function outcomeLabel(
  outcome: DisputeDecisionOutcome | null | undefined,
): string {
  if (outcome === "REQUESTER_FAVORED") {
    return "Requester favored";
  }

  if (outcome === "PROVIDER_FAVORED") {
    return "Provider favored";
  }

  return "Decision pending";
}

function settlementLabel(
  status: ParticipantSettlementStatus,
): string {
  const labels: Record<ParticipantSettlementStatus, string> = {
    AWAITING_DECISION: "Awaiting decision",
    NOT_APPLICABLE: "No token transfer",
    UNSUPPORTED: "Settlement not supported here",
    PENDING: "Settlement pending",
    FAILED: "Transaction failed",
    BLOCKCHAIN_CONFIRMED:
      "Blockchain confirmed · application reconciliation pending",
    CONFIRMED: "Settlement confirmed",
    UNVERIFIED: "Settlement unverified",
  };

  return labels[status];
}

function settlementBadgeVariant(
  status: ParticipantSettlementStatus,
): "default" | "secondary" | "outline" | "destructive" {
  if (
    status === "CONFIRMED" ||
    status === "NOT_APPLICABLE"
  ) {
    return "secondary";
  }

  if (
    status === "FAILED" ||
    status === "UNVERIFIED" ||
    status === "UNSUPPORTED"
  ) {
    return "destructive";
  }

  if (status === "BLOCKCHAIN_CONFIRMED") {
    return "default";
  }

  return "outline";
}

function disputeBadgeVariant(
  status: DisputeCaseStatus,
): "default" | "secondary" | "outline" {
  if (status === "RESOLVED") return "secondary";

  if (
    status === "VOTING" ||
    status === "EVIDENCE_OPEN"
  ) {
    return "default";
  }

  return "outline";
}

function ProgressTimeline({
  item,
}: {
  item: ParticipantDisputeSummary;
}) {
  const decision = isDecisionDeclared(item);

  const votingHasStarted =
    item.votesSubmitted > 0 ||
    item.status === "VOTING" ||
    item.status === "DECIDED" ||
    item.status === "SETTLEMENT_PENDING" ||
    item.status === "RESOLVED";

  const panelAssigned =
    Boolean(item.panelAssignedAt) ||
    item.status !== "PANEL_ASSIGNMENT_PENDING";

  const closureComplete =
    item.status === "RESOLVED" &&
    (
      item.settlementStatus === "CONFIRMED" ||
      item.settlementStatus === "NOT_APPLICABLE"
    );

  const steps = [
    {
      label: "Dispute raised",
      complete: true,
      active: false,
    },
    {
      label: "Panel assigned",
      complete: panelAssigned,
      active: !panelAssigned,
    },
    {
      label: "Voting",
      complete: decision,
      active: votingHasStarted && !decision,
    },
    {
      label: "Decision",
      complete: decision,
      active: false,
    },
    {
      label:
        item.exchangeMode === "DIRECT_BARTER"
          ? "Case closure"
          : "Token settlement",
      complete: closureComplete,
      active: decision && !closureComplete,
    },
  ];

  return (
    <Card>
      <CardHeader className="p-4 pb-3">
        <CardTitle className="text-base">
          Dispute progress
        </CardTitle>

        <p className="text-xs leading-5 text-muted-foreground">
          Follow the case from filing through panel decision
          and any applicable settlement.
        </p>
      </CardHeader>

      <CardContent className="p-4 pt-0">
        <div className="grid grid-cols-2 gap-2 sm:grid-cols-3 xl:grid-cols-5">
          {steps.map((step, index) => (
            <div
              key={step.label}
              className="flex min-w-0 items-start gap-2 rounded-lg border p-3"
            >
              <div className="mt-0.5 shrink-0">
                {step.complete ? (
                  <CheckCircle2
                    size={17}
                    className="text-primary"
                  />
                ) : step.active ? (
                  <Clock3
                    size={17}
                    className="text-primary"
                  />
                ) : (
                  <Circle
                    size={17}
                    className="text-muted-foreground"
                  />
                )}
              </div>

              <div className="min-w-0">
                <p className="text-xs font-medium">
                  {step.label}
                </p>

                <p className="mt-1 text-[10px] text-muted-foreground">
                  {step.complete
                    ? "Complete"
                    : step.active
                      ? "In progress"
                      : index === 1
                        ? "Waiting"
                        : "Pending"}
                </p>
              </div>
            </div>
          ))}
        </div>

        {item.status === "EVIDENCE_OPEN" &&
          item.votesSubmitted === 0 && (
            <p className="mt-3 text-xs text-muted-foreground">
              The panel has been assigned. Voting progress will
              update when a valid vote is submitted.
            </p>
          )}
      </CardContent>
    </Card>
  );
}

function VoteProgress({
  item,
}: {
  item: ParticipantDisputeSummary;
}) {
  const final = isDecisionDeclared(item);
  const panelSize = Math.max(item.panelSize || 5, 1);
  const votesSubmitted = Math.max(item.votesSubmitted || 0, 0);

  const percentage = Math.min(
    (votesSubmitted / panelSize) * 100,
    100,
  );

  return (
    <Card>
      <CardHeader className="p-4 pb-3">
        <div className="flex items-center justify-between gap-3">
          <CardTitle className="flex items-center gap-2 text-base">
            <Vote size={18} className="text-primary" />
            Panel voting progress
          </CardTitle>

          <Badge variant={final ? "secondary" : "outline"}>
            {final ? "Decision declared" : "Anonymous"}
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-3 p-4 pt-0">
        <div className="flex items-end justify-between gap-3">
          <div>
            <p className="text-2xl font-bold tabular-nums">
              {votesSubmitted}
              <span className="text-muted-foreground">
                /{panelSize}
              </span>
            </p>

            <p className="mt-1 text-xs text-muted-foreground">
              {final
                ? "Panel votes recorded when the decision was declared"
                : "Panel votes submitted"}
            </p>
          </div>

          <Users
            size={23}
            className="mb-1 text-muted-foreground"
          />
        </div>

        <div
          className="h-2 overflow-hidden rounded-full bg-muted"
          role="progressbar"
          aria-label="Panel voting progress"
          aria-valuemin={0}
          aria-valuemax={panelSize}
          aria-valuenow={Math.min(votesSubmitted, panelSize)}
        >
          <div
            className="h-full rounded-full bg-primary transition-all"
            style={{ width: `${percentage}%` }}
          />
        </div>

        {final ? (
          <>
            <p className="text-sm font-semibold">
              {outcomeLabel(item.decisionOutcome)}
            </p>

            <div className="grid gap-2 sm:grid-cols-2">
              <div className="rounded-lg border p-3">
                <p className="text-xs text-muted-foreground">
                  Requester · {item.requesterName}
                </p>

                <p className="mt-1 text-xl font-semibold tabular-nums">
                  {item.requesterVotes ?? 0}
                </p>

                <p className="text-xs text-muted-foreground">
                  votes for this side
                </p>
              </div>

              <div className="rounded-lg border p-3">
                <p className="text-xs text-muted-foreground">
                  Provider · {item.providerName}
                </p>

                <p className="mt-1 text-xl font-semibold tabular-nums">
                  {item.providerVotes ?? 0}
                </p>

                <p className="text-xs text-muted-foreground">
                  votes for this side
                </p>
              </div>
            </div>

            <p className="text-xs leading-5 text-muted-foreground">
              Only anonymous aggregate totals are shown.
              Panelist names and seat numbers are never displayed.
            </p>
          </>
        ) : (
          <p className="text-xs leading-5 text-muted-foreground">
            Vote preferences and reasons remain private while
            voting is open. The total updates without showing
            which side individual panelists support.
          </p>
        )}
      </CardContent>
    </Card>
  );
}

function AnonymousRationales({
  item,
}: {
  item: ParticipantDisputeSummary;
}) {
  if (!isDecisionDeclared(item)) return null;

  const reasons = item.anonymousVoteRationales ?? [];

  const requesterReasons = reasons.filter(
    (entry) => entry.outcome === "REQUESTER_FAVORED",
  );

  const providerReasons = reasons.filter(
    (entry) => entry.outcome === "PROVIDER_FAVORED",
  );

  const renderGroup = (
    title: string,
    group: AnonymousPanelVoteRationale[],
  ) => (
    <section className="space-y-2">
      <h3 className="text-sm font-semibold">{title}</h3>

      {group.length === 0 ? (
        <p className="rounded-lg border border-dashed p-3 text-xs text-muted-foreground">
          No recorded rationale for this outcome.
        </p>
      ) : (
        group.map((entry, index) => (
          <article
            key={`${entry.outcome}-${index}`}
            className="rounded-lg border p-3"
          >
            <p className="text-[10px] font-medium uppercase tracking-wide text-muted-foreground">
              Anonymous panel rationale {index + 1}
            </p>

            <p className="mt-2 whitespace-pre-wrap break-words text-sm leading-5">
              {entry.reason}
            </p>
          </article>
        ))
      )}
    </section>
  );

  return (
    <Card>
      <CardHeader className="p-4 pb-3">
        <CardTitle className="flex items-center gap-2 text-base">
          <FileText size={18} className="text-primary" />
          Anonymous panel reasoning
        </CardTitle>

        <p className="text-xs leading-5 text-muted-foreground">
          Reasons are shown only after the decision is declared.
          Panelist names, account details and seat numbers are
          withheld.
        </p>
      </CardHeader>

      <CardContent className="space-y-4 p-4 pt-0">
        {renderGroup(
          `Requester favored · ${item.requesterVotes ?? 0} votes`,
          requesterReasons,
        )}

        {renderGroup(
          `Provider favored · ${item.providerVotes ?? 0} votes`,
          providerReasons,
        )}
      </CardContent>
    </Card>
  );
}

function SettlementTracking({
  item,
}: {
  item: ParticipantDisputeSummary;
}) {
  const tokenCase =
    item.exchangeMode === "TOKEN_BASED" ||
    item.exchangeMode === "HYBRID";

  return (
    <Card>
      <CardHeader className="p-4 pb-3">
        <div className="flex items-center justify-between gap-3">
          <CardTitle className="flex items-center gap-2 text-base">
            <Coins size={19} className="text-primary" />
            Token settlement
          </CardTitle>

          <Badge
            variant={settlementBadgeVariant(item.settlementStatus)}
          >
            {settlementLabel(item.settlementStatus)}
          </Badge>
        </div>
      </CardHeader>

      <CardContent className="space-y-3 p-4 pt-0">
        {tokenCase && item.tokenAmount != null && (
          <div className="grid gap-2 sm:grid-cols-2">
            <div className="rounded-lg border p-3">
              <p className="text-xs text-muted-foreground">
                Agreed amount
              </p>

              <p className="mt-1 text-xl font-semibold tabular-nums">
                {item.tokenAmount} BLT
              </p>
            </div>

            <div className="rounded-lg border p-3">
              <p className="text-xs text-muted-foreground">
                Decision direction
              </p>

              <p className="mt-1 text-sm font-semibold">
                {item.settlementDirection ?? "Not determined yet"}
              </p>

              {item.settlementRecipientName && (
                <p className="mt-1 text-xs text-muted-foreground">
                  Intended recipient: {item.settlementRecipientName}
                </p>
              )}
            </div>
          </div>
        )}

        <p className="text-sm leading-6 text-muted-foreground">
          {item.settlementMessage}
        </p>

        {item.blockchainTransactionStatus && (
          <div className="flex flex-wrap items-center gap-2 text-xs">
            <span className="text-muted-foreground">
              Tracked transaction status:
            </span>

            <Badge variant="outline">
              {formatStatus(item.blockchainTransactionStatus)}
            </Badge>
          </div>
        )}

        {item.blockchainTransactionHash && (
          <div className="rounded-lg border p-3">
            <p className="text-xs text-muted-foreground">
              Blockchain transaction
            </p>

            <p className="mt-1 break-all font-mono text-xs">
              {item.blockchainTransactionHash}
            </p>

            {item.blockchainTransactionUrl && (
              <a
                href={item.blockchainTransactionUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="mt-2 inline-flex items-center gap-1.5 text-sm text-primary underline underline-offset-4"
              >
                View transaction in explorer
                <ExternalLink size={13} />
              </a>
            )}
          </div>
        )}

        {!tokenCase &&
          item.exchangeMode === "DIRECT_BARTER" && (
            <p className="rounded-lg bg-muted/40 p-3 text-xs leading-5">
              This agreement uses Direct Barter; no BLT token
              transfer is expected from the dispute settlement.
            </p>
          )}
      </CardContent>
    </Card>
  );
}

export default function MyDisputesPage() {
  const [cases, setCases] = useState<
    ParticipantDisputeSummary[]
  >([]);

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [isDetailsExpanded, setIsDetailsExpanded] = useState(true);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const loadCases = useCallback(async () => {
    setError(null);
    setLoading(true);

    try {
      const result = await getMyDisputes();

      setCases(result);

      setSelectedId((current) =>
        current &&
        result.some((item) => item.disputeId === current)
          ? current
          : result[0]?.disputeId ?? null,
      );
    } catch (loadError: unknown) {
      setError(
        getErrorMessage(
          loadError,
          "Unable to load your disputes.",
        ),
      );

      setCases([]);
      setSelectedId(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadCases();
  }, [loadCases]);

  const selected =
    cases.find((item) => item.disputeId === selectedId) ?? null;

  const inProgressCount = cases.filter(
    (item) =>
      item.status === "PANEL_ASSIGNMENT_PENDING" ||
      item.status === "EVIDENCE_OPEN" ||
      item.status === "VOTING" ||
      item.status === "DECIDED" ||
      item.status === "SETTLEMENT_PENDING",
  ).length;

  const decisionCount =
    cases.filter(isDecisionDeclared).length;

  const handleRefresh = async () => {
    setRefreshing(true);

    try {
      await loadCases();
    } finally {
      setRefreshing(false);
    }
  };

  return (
    <div className="w-full min-w-0 space-y-5">
      <header className="flex flex-col justify-between gap-3 sm:flex-row sm:items-center">
        <div>
          <div className="flex items-center gap-2.5">
            <Scale size={25} className="text-primary" />
            <h1 className="text-2xl font-bold tracking-tight">
              My Disputes
            </h1>
          </div>

          <p className="mt-1.5 max-w-3xl text-sm leading-5 text-muted-foreground">
            Track your dispute, see anonymous panel progress,
            review the final decision, and verify any token settlement.
          </p>
        </div>

        <Button
          variant="outline"
          size="sm"
          onClick={() => void handleRefresh()}
          disabled={refreshing || loading}
        >
          <RefreshCw
            size={15}
            className={refreshing ? "animate-spin" : ""}
          />
          Refresh
        </Button>
      </header>

      <div className="grid grid-cols-3 gap-2.5">
        <div className="flex min-w-0 items-center gap-2.5 rounded-xl border bg-card px-3 py-3">
          <Scale size={19} className="shrink-0 text-primary" />

          <div className="min-w-0">
            <p className="truncate text-xs text-muted-foreground">
              Total cases
            </p>

            <p className="text-xl font-semibold tabular-nums">
              {cases.length}
            </p>
          </div>
        </div>

        <div className="flex min-w-0 items-center gap-2.5 rounded-xl border bg-card px-3 py-3">
          <Clock3 size={19} className="shrink-0 text-primary" />

          <div className="min-w-0">
            <p className="truncate text-xs text-muted-foreground">
              In progress
            </p>

            <p className="text-xl font-semibold tabular-nums">
              {inProgressCount}
            </p>
          </div>
        </div>

        <div className="flex min-w-0 items-center gap-2.5 rounded-xl border bg-card px-3 py-3">
          <CheckCircle2
            size={19}
            className="shrink-0 text-primary"
          />

          <div className="min-w-0">
            <p className="truncate text-xs text-muted-foreground">
              Decision declared
            </p>

            <p className="text-xl font-semibold tabular-nums">
              {decisionCount}
            </p>
          </div>
        </div>
      </div>

      <div className="grid min-w-0 grid-cols-1 items-start gap-4 xl:grid-cols-[280px_minmax(0,1fr)]">
        <Card className="min-w-0 xl:sticky xl:top-4">
          <CardHeader className="p-4 pb-3">
            <CardTitle className="flex items-center gap-2 text-base">
              <FileText size={18} />
              Your cases
            </CardTitle>
          </CardHeader>

          <CardContent className="space-y-2 p-3 pt-0">
            {loading && (
              <p className="py-6 text-center text-sm text-muted-foreground">
                Loading your disputes…
              </p>
            )}

            {!loading && error && (
              <div className="rounded-lg border border-destructive/40 p-3">
                <p className="flex items-center gap-2 text-sm font-medium text-destructive">
                  <AlertCircle size={16} />
                  Could not load disputes
                </p>

                <p className="mt-2 break-words text-xs leading-5">
                  {error}
                </p>

                <Button
                  className="mt-3 w-full"
                  variant="outline"
                  size="sm"
                  onClick={() => void loadCases()}
                >
                  Try again
                </Button>
              </div>
            )}

            {!loading && !error && cases.length === 0 && (
              <div className="py-7 text-center">
                <ShieldCheck
                  size={26}
                  className="mx-auto text-muted-foreground"
                />

                <p className="mt-2 text-sm font-medium">
                  No disputes yet
                </p>

                <p className="mt-1 text-xs leading-5 text-muted-foreground">
                  Disputes you raised or are involved in will appear here.
                </p>
              </div>
            )}

            {!loading &&
              !error &&
              cases.map((item) => {
                const active = item.disputeId === selectedId;
                const final = isDecisionDeclared(item);

                return (
                  <button
                    key={item.disputeId}
                    type="button"
                    aria-pressed={active}
                    onClick={() => {
  if (selectedId !== item.disputeId) {
    setIsDetailsExpanded(true);
  }

  setSelectedId(item.disputeId);
}}
                    className={`w-full min-w-0 rounded-lg border p-3 text-left transition-colors ${
                      active
                        ? "border-primary bg-primary/5"
                        : "border-border hover:bg-muted/40"
                    }`}
                  >
                    <div className="flex items-start justify-between gap-2">
                      <p className="text-sm font-semibold">
                        Dispute case
                      </p>

                      <Badge
                        variant={disputeBadgeVariant(item.status)}
                        className="shrink-0 text-[10px]"
                      >
                        {formatStatus(item.status)}
                      </Badge>
                    </div>

                    <p className="mt-1.5 break-all font-mono text-[10px] leading-4 text-muted-foreground">
                      {item.disputeId}
                    </p>

                    <p className="mt-2 line-clamp-2 break-words text-xs leading-5 text-muted-foreground">
                      {item.reason}
                    </p>

                    <div className="mt-3 flex flex-wrap items-center gap-2 text-[10px] text-muted-foreground">
                      <span>
                        {item.votesSubmitted}/{item.panelSize} votes
                      </span>

                      <span aria-hidden="true">·</span>

                      <span>
                        {final
                          ? outcomeLabel(item.decisionOutcome)
                          : "Outcomes private"}
                      </span>
                    </div>
                  </button>
                );
              })}
          </CardContent>
        </Card>

        <div className="min-w-0 space-y-3">
  {/* Compact header remains visible when details are collapsed */}
  <div className="flex min-w-0 items-center justify-between gap-3 rounded-xl border bg-card p-3 sm:p-4">
    <div className="min-w-0 flex-1">
      <div className="flex flex-wrap items-center gap-2">
        <Scale size={18} className="shrink-0 text-primary" />

        <h2 className="text-sm font-semibold">
          Dispute details
        </h2>

        {selected && (
          <Badge variant={disputeBadgeVariant(selected.status)}>
            {formatStatus(selected.status)}
          </Badge>
        )}
      </div>

      <p className="mt-1 break-all font-mono text-xs text-muted-foreground">
        {selected?.disputeId ?? "Select a dispute"}
      </p>

      {selected && (
        <p className="mt-1 text-xs text-muted-foreground">
          {selected.votesSubmitted}/{selected.panelSize} panel votes submitted
          {" · "}
          Evidence v{selected.evidenceVersion}
        </p>
      )}
    </div>

    <Button
      type="button"
      variant="outline"
      size="sm"
      disabled={!selected}
      aria-expanded={isDetailsExpanded}
      aria-controls="my-dispute-details-content"
      onClick={() =>
        setIsDetailsExpanded((current) => !current)
      }
      className="shrink-0"
    >
      <ChevronDown
        size={16}
        className={`transition-transform duration-200 ${
          isDetailsExpanded ? "rotate-180" : ""
        }`}
      />

      {isDetailsExpanded ? "Collapse" : "Expand"}
    </Button>
  </div>

  {/* All selected-dispute sections collapse together */}
  <div
    id="my-dispute-details-content"
    className={
      isDetailsExpanded
        ? "min-w-0 space-y-4"
        : "hidden"
    }
  >
          {!loading && !error && selected && (
            <>
              <Card>
                <CardHeader className="p-4 pb-3">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0">
                      <CardTitle className="text-base">
                        Dispute overview
                      </CardTitle>

                      <p className="mt-1.5 break-all font-mono text-xs text-muted-foreground">
                        {selected.disputeId}
                      </p>
                    </div>

                    <Badge variant={disputeBadgeVariant(selected.status)}>
                      {formatStatus(selected.status)}
                    </Badge>
                  </div>
                </CardHeader>

                <CardContent className="space-y-4 p-4 pt-0">
                  <div>
                    <p className="text-xs font-medium text-muted-foreground">
                      Reason submitted
                    </p>

                    <p className="mt-1 whitespace-pre-wrap break-words text-sm leading-6">
                      {selected.reason}
                    </p>
                  </div>

                  <div className="grid gap-2 sm:grid-cols-2">
                    <div className="rounded-lg border p-3">
                      <p className="text-xs text-muted-foreground">
                        Claimant
                      </p>

                      <p className="mt-1 text-sm font-semibold">
                        {selected.claimantName}
                      </p>

                      {selected.currentUserRole === "CLAIMANT" && (
                        <p className="mt-1 text-[11px] text-primary">
                          You raised this dispute
                        </p>
                      )}
                    </div>

                    <div className="rounded-lg border p-3">
                      <p className="text-xs text-muted-foreground">
                        Respondent
                      </p>

                      <p className="mt-1 text-sm font-semibold">
                        {selected.respondentName}
                      </p>

                      {selected.currentUserRole === "RESPONDENT" && (
                        <p className="mt-1 text-[11px] text-primary">
                          You are responding to this dispute
                        </p>
                      )}
                    </div>
                  </div>

                  <div className="flex flex-wrap gap-x-4 gap-y-1 border-t pt-3 text-xs text-muted-foreground">
                    <span>
                      Opened: {formatDate(selected.createdAt)}
                    </span>

                    <span>
                      Evidence revision: v{selected.evidenceVersion}
                    </span>

                    <span>
                      Exchange: {formatStatus(selected.exchangeMode)}
                    </span>
                  </div>
                </CardContent>
              </Card>

              <ProgressTimeline item={selected} />

              <VoteProgress item={selected} />

              {isDecisionDeclared(selected) && (
                <Card>
                  <CardHeader className="p-4 pb-3">
                    <CardTitle className="flex items-center gap-2 text-base">
                      <CheckCircle2
                        size={18}
                        className="text-primary"
                      />
                      Final decision
                    </CardTitle>
                  </CardHeader>

                  <CardContent className="space-y-3 p-4 pt-0">
                    <div className="rounded-lg border p-3">
                      <p className="text-xs text-muted-foreground">
                        Outcome
                      </p>

                      <p className="mt-1 text-lg font-semibold">
                        {outcomeLabel(selected.decisionOutcome)}
                      </p>

                      {selected.decisionDeclaredAt && (
                        <p className="mt-1 text-xs text-muted-foreground">
                          Declared: {formatDate(selected.decisionDeclaredAt)}
                        </p>
                      )}
                    </div>

                    {selected.decisionSummary && (
                      <p className="whitespace-pre-wrap break-words text-sm leading-6">
                        {selected.decisionSummary}
                      </p>
                    )}

                    <p className="text-xs text-muted-foreground">
                      The decision is read-only. No appeal action is
                      provided by the current workflow.
                    </p>
                  </CardContent>
                </Card>
              )}

              <AnonymousRationales item={selected} />

              <SettlementTracking item={selected} />
            </>
          )}

          {!loading && !error && !selected && (
            <Card>
              <CardContent className="flex min-h-56 flex-col items-center justify-center p-8 text-center">
                <ShieldCheck
                  size={30}
                  className="text-muted-foreground"
                />

                <h2 className="mt-3 text-sm font-semibold">
                  Select a dispute
                </h2>

                <p className="mt-1 max-w-sm text-xs leading-5 text-muted-foreground">
                  Case progress, anonymous vote totals, the final
                  reasoning, and verified settlement details will
                  appear here.
                </p>
              </CardContent>
          </Card>
          )}
          </div>
        </div>
      </div>

      <p className="text-[11px] leading-5 text-muted-foreground">
        Panelist identities and individual vote preferences stay private
        while voting is open. Token settlement is shown as confirmed only
        when the tracked blockchain transaction and application finalization
        states agree.
      </p>
    </div>
  );
}