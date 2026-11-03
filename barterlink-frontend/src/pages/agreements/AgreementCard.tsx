import { useState } from "react";

import {
  AlertTriangle,
  Calendar,
  CheckCircle2,
  ChevronDown,
  ChevronUp,
  Clock,
  Coins,
  FileText,
  Handshake,
  History,
  Layers,
  RefreshCw,
  User,
} from "lucide-react";

import type Agreement from "@/models/agreement/Agreement";

import type AgreementVersion from "@/models/agreement/AgreementVersion";

import {
  getAgreementVersions,
} from "@/services/AgreementService";

import {
  Card,
  CardContent,
} from "@/components/ui/card";

import {
  Badge,
} from "@/components/ui/badge";

import {
  Button,
} from "@/components/ui/button";

interface AgreementCardProps {
  agreement: Agreement;
}

const formatDate = (
  value?: string | null,
): string => {
  if (!value) {
    return "Not specified";
  }

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? "Not specified"
    : date.toLocaleDateString();
};

const formatDateTime = (
  value?: string | null,
): string => {
  if (!value) {
    return "Not recorded";
  }

  const date = new Date(value);

  return Number.isNaN(date.getTime())
    ? "Not recorded"
    : date.toLocaleString();
};

const formatLabel = (
  value?: string | null,
): string => {
  if (!value) {
    return "Not specified";
  }

  return value
    .replace(/_/g, " ")
    .toLowerCase()
    .replace(/\b\w/g, (character) =>
      character.toUpperCase(),
    );
};

const formatTokens = (
  amount?: number | null,
): string => {
  if (
    typeof amount !== "number" ||
    !Number.isFinite(amount)
  ) {
    return "Not specified";
  }

  return `${amount.toLocaleString()} ${
    amount === 1 ? "Token" : "Tokens"
  }`;
};

const exchangeModeClass = (
  mode: Agreement["exchangeMode"],
): string => {
  switch (mode) {
    case "DIRECT_BARTER":
      return "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300";

    case "TOKEN_BASED":
      return "border-sky-500/30 bg-sky-500/10 text-sky-700 dark:text-sky-300";

    case "HYBRID":
      return "border-violet-500/30 bg-violet-500/10 text-violet-700 dark:text-violet-300";

    default:
      return "";
  }
};

const versionStatusClass = (
  status: AgreementVersion["status"],
): string => {
  switch (status) {
    case "ACTIVE":
      return "border-emerald-500/30 bg-emerald-500/10 text-emerald-700 dark:text-emerald-300";

    case "PENDING_ACCEPTANCE":
      return "border-amber-500/30 bg-amber-500/10 text-amber-700 dark:text-amber-300";

    case "READY_FOR_ACTIVATION":
      return "border-sky-500/30 bg-sky-500/10 text-sky-700 dark:text-sky-300";

    case "SUPERSEDED":
      return "border-muted bg-muted text-muted-foreground";

    case "REJECTED":
    case "FUNDING_FAILED":
      return "border-destructive/30 bg-destructive/10 text-destructive";

    default:
      return "";
  }
};

function AgreementCard({
  agreement,
}: AgreementCardProps) {
  const [expanded, setExpanded] =
    useState(false);

  const [versions, setVersions] =
    useState<AgreementVersion[]>([]);

  const [versionsLoading, setVersionsLoading] =
    useState(false);

  const [versionsLoaded, setVersionsLoaded] =
    useState(false);

  const [versionsError, setVersionsError] =
    useState<string | null>(null);

  const loadVersions = async () => {
    if (versionsLoading) {
      return;
    }

    setVersionsLoading(true);
    setVersionsError(null);

    try {
      const result = await getAgreementVersions(
        agreement.negotiationId,
      );

      setVersions(result);
      setVersionsLoaded(true);
    } catch (error: unknown) {
      const message = (
        error as {
          response?: {
            data?: {
              message?: string;
            };
          };
        }
      )?.response?.data?.message;

      setVersionsError(
        message ??
          "Could not load agreement versions. Please try again.",
      );
    } finally {
      setVersionsLoading(false);
    }
  };

  const handleToggleVersions = async () => {
    const willExpand = !expanded;

    setExpanded(willExpand);

    if (willExpand && !versionsLoaded) {
      await loadVersions();
    }
  };

  const isDirectBarter =
    agreement.exchangeMode === "DIRECT_BARTER";

  const exchangeModeLabel =
    formatLabel(agreement.exchangeMode);

  return (
    <Card className="overflow-hidden transition-all duration-200 hover:shadow-lg">
      <CardContent className="space-y-6 p-5 sm:p-6">

        {/* Agreement header */}

        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="min-w-0 space-y-3">

            <div className="flex flex-wrap items-center gap-2">
              <Badge
                variant="outline"
                className={exchangeModeClass(
                  agreement.exchangeMode,
                )}
              >
                <Handshake className="mr-1 h-3.5 w-3.5" />
                {exchangeModeLabel}
              </Badge>

              <Badge variant="secondary">
                {formatLabel(agreement.status)}
              </Badge>
            </div>

            <div>
              <h3 className="text-xl font-semibold">
                {agreement.offerTitle ||
                  (isDirectBarter
                    ? "Direct skill exchange"
                    : "Service agreement")}
              </h3>

              <p className="mt-1 text-sm text-muted-foreground">
                Looking for{" "}
                <span className="font-medium text-foreground">
                  {agreement.requestTitle ||
                    "Requested service not specified"}
                </span>
              </p>
            </div>
          </div>

          <Button
            type="button"
            variant="outline"
            onClick={() =>
              void handleToggleVersions()
            }
            disabled={versionsLoading && !expanded}
            className="shrink-0"
          >
            <History className="mr-2 h-4 w-4" />

            {expanded
              ? "Hide versions"
              : "View versions"}

            {expanded ? (
              <ChevronUp className="ml-2 h-4 w-4" />
            ) : (
              <ChevronDown className="ml-2 h-4 w-4" />
            )}
          </Button>
        </div>

        {/* Participants */}

        <div className="grid gap-3 sm:grid-cols-2">

          <div className="rounded-lg border p-4">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <User className="h-4 w-4" />
              Requester
            </div>

            <p className="mt-2 font-semibold">
              {agreement.requesterName}
            </p>
          </div>

          <div className="rounded-lg border p-4">
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <User className="h-4 w-4" />
              Provider
            </div>

            <p className="mt-2 font-semibold">
              {agreement.providerName}
            </p>
          </div>

        </div>

        {/* Agreement terms */}

        <div className="grid gap-4 md:grid-cols-2">

          <div className="rounded-lg border p-4">
            <h4 className="mb-2 font-semibold text-emerald-600">
              Offering
            </h4>

            <p className="font-medium">
              {agreement.offerTitle ||
                (isDirectBarter
                  ? "Skill or service exchange"
                  : "Offer details not specified")}
            </p>

            <p className="mt-2 whitespace-pre-wrap text-sm text-muted-foreground">
              {agreement.offerDescription ||
                "No additional offering description."}
            </p>
          </div>

          <div className="rounded-lg border p-4">
            <h4 className="mb-2 font-semibold text-sky-600">
              Looking For
            </h4>

            <p className="font-medium">
              {agreement.requestTitle ||
                "Requested service not specified"}
            </p>

            <p className="mt-2 whitespace-pre-wrap text-sm text-muted-foreground">
              {agreement.requestDescription ||
                "No additional request description."}
            </p>
          </div>

        </div>

        {/* Agreement facts */}

        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">

          <div className="rounded-lg bg-muted/40 p-3">
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Layers className="h-4 w-4" />
              Release mode
            </div>

            <p className="mt-2 text-sm font-semibold">
              {isDirectBarter
                ? "Not applicable"
                : formatLabel(agreement.releaseMode)}
            </p>
          </div>

          <div className="rounded-lg bg-muted/40 p-3">
            <div className="flex items-center gap-2 text-xs text-muted-foreground">
              <Clock className="h-4 w-4" />
              Estimated effort
            </div>

            <p className="mt-2 text-sm font-semibold">
              {agreement.estimatedHours != null
                ? `${agreement.estimatedHours} hours`
                : "Not specified"}
            </p>
          </div>

          {!isDirectBarter && (
            <>
              <div className="rounded-lg bg-muted/40 p-3">
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <Coins className="h-4 w-4" />
                  Agreed amount
                </div>

                <p className="mt-2 text-sm font-semibold">
                  {formatTokens(
                    agreement.agreedTokenAmount,
                  )}
                </p>
              </div>

              <div className="rounded-lg bg-muted/40 p-3">
                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                  <Coins className="h-4 w-4" />
                  Locked in escrow
                </div>

                <p className="mt-2 text-sm font-semibold">
                  {formatTokens(
                    agreement.escrowLockedTokens,
                  )}
                </p>
              </div>
            </>
          )}

        </div>

        <div className="flex flex-wrap items-center gap-x-5 gap-y-2 border-t pt-4 text-sm text-muted-foreground">

          <div className="flex items-center gap-2">
            <Calendar className="h-4 w-4" />

            <span>
              Accepted: {formatDate(agreement.acceptedAt)}
            </span>
          </div>

          {agreement.completedAt && (
            <div className="flex items-center gap-2">
              <CheckCircle2 className="h-4 w-4" />

              <span>
                Completed: {formatDate(agreement.completedAt)}
              </span>
            </div>
          )}

        </div>

        {/* Version history */}

        {expanded && (
          <div className="space-y-4 border-t pt-5">

            <div className="flex items-center justify-between gap-3">
              <div>
                <div className="flex items-center gap-2">
                  <History className="h-5 w-5 text-primary" />

                  <h4 className="text-lg font-semibold">
                    Agreement version history
                  </h4>
                </div>

                <p className="mt-1 text-sm text-muted-foreground">
                  Version-specific terms, acceptance, and milestone details.
                </p>
              </div>

              {versionsLoaded && (
                <Badge variant="secondary">
                  {versions.length}{" "}
                  {versions.length === 1
                    ? "version"
                    : "versions"}
                </Badge>
              )}
            </div>

            {versionsLoading ? (
              <div className="flex items-center justify-center gap-3 rounded-lg border p-8">
                <RefreshCw className="h-5 w-5 animate-spin" />

                <p className="text-sm text-muted-foreground">
                  Loading agreement versions...
                </p>
              </div>
            ) : versionsError ? (
              <div className="space-y-3 rounded-lg border border-destructive/30 p-4">
                <div className="flex items-start gap-2">
                  <AlertTriangle className="mt-0.5 h-5 w-5 text-destructive" />

                  <p className="text-sm">
                    {versionsError}
                  </p>
                </div>

                <Button
                  type="button"
                  size="sm"
                  variant="outline"
                  onClick={() => void loadVersions()}
                  disabled={versionsLoading}
                >
                  <RefreshCw className="mr-2 h-4 w-4" />
                  Try again
                </Button>
              </div>
            ) : versions.length === 0 ? (
              <div className="rounded-lg border border-dashed p-8 text-center">
                <FileText className="mx-auto mb-3 h-8 w-8 text-muted-foreground" />

                <h5 className="font-semibold">
                  No version history available
                </h5>

                <p className="mt-1 text-sm text-muted-foreground">
                  This agreement has no version records available through the version-history endpoint.
                </p>
              </div>
            ) : (
              <div className="space-y-4">

                {versions.map((version, index) => {
                  const milestones =
                    version.milestones ?? [];

                  return (
                    <div
                      key={version.versionId}
                      className="space-y-4 rounded-xl border p-4 sm:p-5"
                    >

                      {/* Version heading */}

                      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">

                        <div>
                          <div className="flex flex-wrap items-center gap-2">
                            <h5 className="text-base font-semibold">
                              Version {version.versionNumber}
                            </h5>

                            <Badge
                              variant="outline"
                              className={versionStatusClass(
                                version.status,
                              )}
                            >
                              {formatLabel(version.status)}
                            </Badge>

                            {version.status === "ACTIVE" ? (
                              <Badge className="bg-emerald-600 text-white">
                                Active version
                              </Badge>
                            ) : index === 0 ? (
                              <Badge variant="secondary">
                                Latest version
                              </Badge>
                            ) : null}
                          </div>

                          <p className="mt-2 text-sm text-muted-foreground">
                            Created {formatDateTime(version.createdAt)}
                          </p>
                        </div>

                        <Badge
                          variant="outline"
                          className={exchangeModeClass(
                            version.exchangeMode,
                          )}
                        >
                          {formatLabel(version.exchangeMode)}
                        </Badge>

                      </div>

                      {/* Version change summary */}

                      <div className="rounded-lg bg-muted/40 p-3">
                        <p className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
                          Change summary
                        </p>

                        <p className="mt-1 text-sm">
                          {version.changeSummary ||
                            "No change summary provided."}
                        </p>
                      </div>

                      {/* Version-specific service terms */}

                      <div className="grid gap-3 md:grid-cols-2">

                        <div className="rounded-lg border p-3">
                          <h6 className="mb-2 text-sm font-semibold text-emerald-600">
                            Offering
                          </h6>

                          <p className="text-sm font-medium">
                            {version.offerTitle ||
                              (version.exchangeMode === "TOKEN_BASED"
                                ? "Token-based service"
                                : "Offer details not specified")}
                          </p>

                          <p className="mt-1 whitespace-pre-wrap text-sm text-muted-foreground">
                            {version.offerDescription ||
                              "No additional offering description."}
                          </p>
                        </div>

                        <div className="rounded-lg border p-3">
                          <h6 className="mb-2 text-sm font-semibold text-sky-600">
                            Looking For
                          </h6>

                          <p className="text-sm font-medium">
                            {version.requestTitle}
                          </p>

                          <p className="mt-1 whitespace-pre-wrap text-sm text-muted-foreground">
                            {version.requestDescription}
                          </p>
                        </div>

                      </div>

                      {/* Agreed terms */}

                      {version.agreedTerms && (
                        <div className="rounded-lg border p-3">
                          <h6 className="mb-2 font-semibold">
                            Agreed terms
                          </h6>

                          <p className="whitespace-pre-wrap text-sm text-muted-foreground">
                            {version.agreedTerms}
                          </p>
                        </div>
                      )}

                      {/* Version facts */}

                      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">

                        <div className="rounded-lg bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">
                            Release mode
                          </p>

                          <p className="mt-1 text-sm font-semibold">
                            {version.exchangeMode === "DIRECT_BARTER"
                              ? "Not applicable"
                              : formatLabel(version.releaseMode)}
                          </p>
                        </div>

                        <div className="rounded-lg bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">
                            Agreed token amount
                          </p>

                          <p className="mt-1 text-sm font-semibold">
                            {version.exchangeMode === "DIRECT_BARTER"
                              ? "No token payment"
                              : formatTokens(
                                  version.agreedTokenAmount,
                                )}
                          </p>
                        </div>

                        <div className="rounded-lg bg-muted/40 p-3">
                          <p className="text-xs text-muted-foreground">
                            Estimated effort
                          </p>

                          <p className="mt-1 text-sm font-semibold">
                            {version.estimatedHours} hours
                          </p>
                        </div>

                        <div className="rounded-lg bg-muted/40 p-3 sm:col-span-2 lg:col-span-3">
                          <div className="flex items-center gap-2 text-xs text-muted-foreground">
                            <Calendar className="h-4 w-4" />
                            Agreed deadline
                          </div>

                          <p className="mt-1 text-sm font-semibold">
                            {formatDateTime(
                              version.agreedDeadline,
                            )}
                          </p>
                        </div>

                      </div>

                      {/* Participant acceptance */}

                      <div className="space-y-3">
                        <h6 className="font-semibold">
                          Participant acceptance
                        </h6>

                        <div className="grid gap-3 sm:grid-cols-2">

                          <div className="rounded-lg border p-3">
                            <div className="flex items-center gap-2">
                              {version.requesterAcceptedAt ? (
                                <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                              ) : (
                                <Clock className="h-4 w-4 text-muted-foreground" />
                              )}

                              <span className="text-sm font-medium">
                                Requester
                              </span>
                            </div>

                            <p className="mt-2 text-sm">
                              {version.requesterAcceptedAt
                                ? "Accepted"
                                : "Acceptance not recorded"}
                            </p>

                            {version.requesterAcceptedAt && (
                              <p className="mt-1 text-xs text-muted-foreground">
                                {formatDateTime(
                                  version.requesterAcceptedAt,
                                )}
                              </p>
                            )}
                          </div>

                          <div className="rounded-lg border p-3">
                            <div className="flex items-center gap-2">
                              {version.providerAcceptedAt ? (
                                <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                              ) : (
                                <Clock className="h-4 w-4 text-muted-foreground" />
                              )}

                              <span className="text-sm font-medium">
                                Provider
                              </span>
                            </div>

                            <p className="mt-2 text-sm">
                              {version.providerAcceptedAt
                                ? "Accepted"
                                : "Acceptance not recorded"}
                            </p>

                            {version.providerAcceptedAt && (
                              <p className="mt-1 text-xs text-muted-foreground">
                                {formatDateTime(
                                  version.providerAcceptedAt,
                                )}
                              </p>
                            )}
                          </div>

                        </div>
                      </div>

                      {/* Version lifecycle */}

                      <div className="grid gap-3 sm:grid-cols-2">

                        <div>
                          <p className="text-xs text-muted-foreground">
                            Submitted
                          </p>

                          <p className="mt-1 text-sm">
                            {formatDateTime(
                              version.submittedAt,
                            )}
                          </p>
                        </div>

                        <div>
                          <p className="text-xs text-muted-foreground">
                            Activated
                          </p>

                          <p className="mt-1 text-sm">
                            {formatDateTime(
                              version.activatedAt,
                            )}
                          </p>
                        </div>

                        {version.supersededAt && (
                          <div>
                            <p className="text-xs text-muted-foreground">
                              Superseded
                            </p>

                            <p className="mt-1 text-sm">
                              {formatDateTime(
                                version.supersededAt,
                              )}
                            </p>
                          </div>
                        )}

                      </div>

                      {/* Activation failure */}

                      {version.activationFailureReason && (
                        <div className="rounded-lg border border-amber-500/30 bg-amber-500/5 p-3">
                          <div className="flex items-start gap-2">
                            <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />

                            <div>
                              <p className="text-sm font-semibold">
                                Activation information
                              </p>

                              <p className="mt-1 whitespace-pre-wrap text-sm text-muted-foreground">
                                {version.activationFailureReason}
                              </p>
                            </div>
                          </div>
                        </div>
                      )}

                      {/* Milestone plan */}

                      {milestones.length > 0 && (
                        <div className="space-y-3">
                          <div className="flex items-center justify-between gap-3">
                            <h6 className="font-semibold">
                              Milestone plan
                            </h6>

                            <Badge variant="secondary">
                              {milestones.length}{" "}
                              {milestones.length === 1
                                ? "milestone"
                                : "milestones"}
                            </Badge>
                          </div>

                          <div className="space-y-3">
                            {milestones.map((milestone) => (
                              <div
                                key={milestone.versionMilestoneId}
                                className="rounded-lg border p-3"
                              >
                                <div className="flex flex-col gap-2 sm:flex-row sm:items-start sm:justify-between">
                                  <div>
                                    <p className="font-medium">
                                      {milestone.milestoneNumber}.{" "}
                                      {milestone.title}
                                    </p>

                                    <p className="mt-1 whitespace-pre-wrap text-sm text-muted-foreground">
                                      {milestone.deliverable}
                                    </p>
                                  </div>

                                  <Badge variant="outline">
                                    {formatLabel(
                                      milestone.executionStatus,
                                    )}
                                  </Badge>
                                </div>

                                <div className="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-sm text-muted-foreground">
                                  <span className="flex items-center gap-1.5">
                                    <Calendar className="h-3.5 w-3.5" />
                                    {formatDate(
                                      milestone.deadline,
                                    )}
                                  </span>

                                  <span className="flex items-center gap-1.5">
                                    <Coins className="h-3.5 w-3.5" />
                                    {formatTokens(
                                      milestone.tokenAllocation,
                                    )}
                                  </span>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )}

                    </div>
                  );
                })}

              </div>
            )}

          </div>
        )}

      </CardContent>
    </Card>
  );
}

export default AgreementCard;