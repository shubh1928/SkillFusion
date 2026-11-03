import { useEffect, useMemo, useState } from "react";
import {
  CalendarCheck,
  CheckCircle2,
  Circle,
  Copy,
  Gift,
  Link2,
  UsersRound,
  WalletCards,
} from "lucide-react";
import toast from "react-hot-toast";

import { Button } from "@/components/ui/button";
import { Spinner } from "@/components/ui/spinner";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

import {
  getRewardsOverview,
  type RewardsOverview,
} from "@/services/RewardsService";

function formatDate(value: string) {
  return new Date(value).toLocaleDateString(undefined, {
    day: "numeric",
    month: "short",
    year: "numeric",
  });
}

function claimStatusLabel(status: string) {
  switch (status) {
    case "PENDING":
      return "Pending on-chain";
    case "PROCESSING":
      return "Processing";
    case "SUBMITTED":
      return "Submitted to blockchain";
    case "RECONCILIATION_REQUIRED":
      return "Reconciliation required";
    case "REVIEW_REQUIRED":
      return "Review required";
    case "WAITING_FOR_OTHER_COUNTERPARTY":
  return "Waiting for another participant";
    case "CONFIRMED":
      return "Confirmed";
    case "FAILED":
      return "Failed — needs attention";
    case "DAY_30_REACHED":
      return "Day 30 reached";
    case "IN_PROGRESS":
      return "In progress";
    case "SIGNED_UP":
      return "Signed up";
    case "ONE_OF_TWO":
      return "1/2 qualifying sessions";
    case "QUALIFIED":
      return "Qualified";
    case "LIFETIME_CAP_REACHED":
      return "No reward — lifetime cap reached";
    default:
      return status.replaceAll("_", " ").toLowerCase();
  }
}

function statusVariant(
  status: string
): "default" | "secondary" | "destructive" | "outline" {
  if (status === "CONFIRMED" || status === "REWARD_QUEUED") {
    return "default";
  }

  if (status === "FAILED" || status === "LIFETIME_CAP_REACHED") {
    return "destructive";
  }

  if (
    status === "REVIEW_REQUIRED" ||
    status === "RECONCILIATION_REQUIRED"
  ) {
    return "outline";
  }

  return "secondary";
}

function ReferralPage() {
  const [overview, setOverview] = useState<RewardsOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    let active = true;

    getRewardsOverview()
      .then((data) => {
        if (active) {
          setOverview(data);
        }
      })
      .catch((err) => {
        console.error("Failed to load reward overview", err);

        if (active) {
          setError("We couldn't load your rewards. Please try again.");
        }
      })
      .finally(() => {
        if (active) {
          setLoading(false);
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const referralLink = useMemo(
    () =>
      overview?.referrals.referralCode
        ? `${window.location.origin}/signup?referrerCode=${encodeURIComponent(
            overview.referrals.referralCode
          )}`
        : "",
    [overview?.referrals.referralCode]
  );

  const copy = async (value: string, label: string) => {
    if (!value) {
      return;
    }

    try {
      await navigator.clipboard.writeText(value);
      toast.success(`${label} copied`);
    } catch (err) {
      console.error("Clipboard write failed", err);
      toast.error(
        "Couldn't access the clipboard. Please copy the text manually."
      );
    }
  };

  if (loading) {
    return (
      <div className="flex min-h-[50vh] items-center justify-center gap-3 text-muted-foreground">
        <Spinner />
        Loading your rewards...
      </div>
    );
  }

  if (error || !overview) {
    return (
      <Card className="mx-auto max-w-2xl">
        <CardContent className="p-6">
          <p className="text-sm text-destructive">
            {error || "Rewards are unavailable."}
          </p>

          <Button
            className="mt-4"
            variant="outline"
            onClick={() => window.location.reload()}
          >
            Retry
          </Button>
        </CardContent>
      </Card>
    );
  }

  const streak = overview.dailyStreak;
  const referrals = overview.referrals;

  return (
    <div className="mx-auto max-w-6xl space-y-6 pb-8">
      <div className="flex items-center gap-3">
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-primary">
          <Gift className="h-6 w-6" />
        </div>

        <div>
          <h2 className="text-2xl font-bold">Rewards</h2>
          <p className="text-sm text-muted-foreground">
            Track your Direct Barter, daily streak, and referral campaigns.
          </p>
        </div>
      </div>

      {!overview.payoutsEnabled && (
        <div className="rounded-xl border border-amber-500/30 bg-amber-500/5 p-4 text-sm">
          <strong>Reward payouts are currently disabled.</strong>{" "}
          This page displays recorded progress and claim states; it does not
          activate rewards or mint BLT.
        </div>
      )}

      <div className="grid gap-5 lg:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <WalletCards className="h-5 w-5" />
              Direct Barter
            </CardTitle>
            <CardDescription>
              1 BLT per eligible completed Direct Barter session, per participant.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-3">
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-bold">
                {overview.directBarter.slotsReserved}/
                {overview.directBarter.lifetimeLimit}
              </span>
              <span className="text-sm text-muted-foreground">
                reward slots reserved
              </span>
            </div>

            <div className="h-2 overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-primary"
                style={{
                  width: `${
                    Math.min(
                      100,
                      (overview.directBarter.slotsReserved /
                        overview.directBarter.lifetimeLimit) *
                        100
                    )
                  }%`,
                }}
              />
            </div>

            <p className="text-sm text-muted-foreground">
              {overview.directBarter.slotsRemaining} lifetime slots remaining
            </p>

            {overview.directBarter.slotsRemaining === 0 && (
  <p
    role="status"
    className="text-sm font-medium text-muted-foreground"
  >
    Direct Barter reward campaign completed - lifetime limit reached.
  </p>
)}

            <div className="grid grid-cols-2 gap-2 text-sm">
              <div className="rounded-lg bg-muted/40 p-3">
                <p className="text-muted-foreground">Confirmed</p>
                <p className="text-xl font-semibold">
                  {overview.directBarter.confirmedClaims}
                </p>
              </div>

              <div className="rounded-lg bg-muted/40 p-3">
                <p className="text-muted-foreground">Pending</p>
                <p className="text-xl font-semibold">
                  {overview.directBarter.pendingClaims}
                </p>
              </div>
            </div>

            {(overview.directBarter.reconciliationRequired > 0 ||
              overview.directBarter.reviewRequired > 0 ||
              overview.directBarter.failedClaims > 0) && (
              <p className="text-xs text-amber-700 dark:text-amber-400">
                Needs attention: {overview.directBarter.reconciliationRequired}{" "}
                reconciliation, {overview.directBarter.reviewRequired} review,{" "}
                {overview.directBarter.failedClaims} failed.
              </p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <CalendarCheck className="h-5 w-5" />
              30-day activity streak
            </CardTitle>
            <CardDescription>
              5 BLT once after 30 consecutive qualifying UTC dates.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-4">
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-bold">
                {streak.currentStreakDays}/30
              </span>
              <span className="text-sm text-muted-foreground">
                consecutive days
              </span>
            </div>

            <div
              className="grid grid-cols-6 gap-1.5 sm:grid-cols-10"
              aria-label="Last 30 UTC calendar days"
            >
              {streak.calendar.map((day) => (
                <div
                  key={day.date}
                  title={`${day.date}: ${
                    day.qualified
                      ? "qualified"
                      : `login ${day.loginRecorded ? "yes" : "no"}, activity ${
                          day.qualifyingActivityRecorded ? "yes" : "no"
                        }`
                  }`}
                  className={`flex aspect-square items-center justify-center rounded-md border text-[10px] ${
                    day.qualified
                      ? "border-emerald-500 bg-emerald-500/15 text-emerald-700 dark:text-emerald-300"
                      : day.loginRecorded || day.qualifyingActivityRecorded
                        ? "border-amber-500/50 bg-amber-500/10"
                        : "border-border bg-muted/20 text-muted-foreground"
                  }`}
                >
                  <span>
                    {new Date(`${day.date}T00:00:00Z`).getUTCDate()}
                  </span>
                </div>
              ))}
            </div>

            <div className="space-y-2 text-sm">
              <p className="flex items-center gap-2">
                {streak.loginRecordedToday ? (
                  <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                ) : (
                  <Circle className="h-4 w-4 text-muted-foreground" />
                )}
                Successful login recorded today (UTC)
              </p>

              <p className="flex items-center gap-2">
                {streak.qualifyingActivityRecordedToday ? (
                  <CheckCircle2 className="h-4 w-4 text-emerald-600" />
                ) : (
                  <Circle className="h-4 w-4 text-muted-foreground" />
                )}
                Qualifying activity recorded today (UTC)
              </p>
            </div>

            <Badge variant={statusVariant(streak.rewardStatus)}>
  {streak.rewardStatus === "CONFIRMED"
    ? "5 BLT claimed — lifetime campaign complete"
    : streak.rewardStatus === "DAY_30_REACHED"
      ? "Day 30 reached — reward not yet reserved"
      : claimStatusLabel(streak.rewardStatus)}
</Badge>

            {streak.rewardClaimReserved && (
              <p className="text-xs text-muted-foreground">
                The one-time streak claim is already reserved; its status will
                not create a second reward.
              </p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <UsersRound className="h-5 w-5" />
              Refer &amp; Earn
            </CardTitle>
            <CardDescription>
              3 BLT for each qualified referred friend, up to five lifetime
              reward slots.
            </CardDescription>
          </CardHeader>

          <CardContent className="space-y-3">
            <div className="flex items-baseline gap-2">
              <span className="text-3xl font-bold">
                {referrals.rewardSlotsReserved}/{referrals.lifetimeLimit}
              </span>
              <span className="text-sm text-muted-foreground">
                reward slots reserved
              </span>
            </div>

            <div className="h-2 overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-primary"
                style={{
                  width: `${
                    Math.min(
                      100,
                      (referrals.rewardSlotsReserved /
                        referrals.lifetimeLimit) *
                        100
                    )
                  }%`,
                }}
              />
            </div>

            <p className="text-sm text-muted-foreground">
              {referrals.slotsRemaining} reward slots remaining
            </p>

            <div className="rounded-lg border p-3">
              <p className="mb-1 text-xs text-muted-foreground">
                Your referral code
              </p>
              <p className="break-all font-mono font-semibold">
                {referrals.referralCode}
              </p>

              <Button
                className="mt-2 w-full"
                variant="outline"
                onClick={() =>
                  void copy(referrals.referralCode, "Referral code")
                }
              >
                <Copy className="mr-2 h-4 w-4" />
                Copy code
              </Button>
            </div>

            <div className="space-y-2">
              <p className="text-xs text-muted-foreground">Share link</p>

              <p className="break-all rounded-lg bg-muted/30 p-2 text-xs">
                {referralLink}
              </p>

              <Button
                className="w-full"
                variant="outline"
                onClick={() => void copy(referralLink, "Referral link")}
              >
                <Link2 className="mr-2 h-4 w-4" />
                Copy link
              </Button>
            </div>

           <p className="text-xs text-muted-foreground">
  Confirmed: {referrals.confirmedRewards} · Pending:{" "}
  {referrals.pendingRewards} · Waiting for another participant:{" "}
  {referrals.waitingForOtherCounterparty} · Failed:{" "}
  {referrals.failedRewards}
</p>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Referred friends</CardTitle>
          <CardDescription>
            Only a masked account label, attribution date, qualification
            progress, and reward status are displayed.
          </CardDescription>
        </CardHeader>

        <CardContent>
          {referrals.invitees.length === 0 ? (
            <p className="py-4 text-sm text-muted-foreground">
              No referral signups recorded yet. Share your code to invite a new
              user.
            </p>
          ) : (
            <div className="space-y-3">
              {referrals.invitees.map((invitee, index) => (
                <div
                  key={`${invitee.maskedLabel}-${invitee.attributedAt}-${index}`}
                  className="flex flex-col gap-2 rounded-lg border p-3 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div>
                    <p className="font-medium">{invitee.maskedLabel}</p>
                    <p className="text-xs text-muted-foreground">
                      Joined {formatDate(invitee.attributedAt)} ·{" "}
                      {invitee.qualifyingSessions}/2 qualifying Direct Barter
                      sessions
                    </p>
                  </div>

                  <div className="flex items-center gap-2">
                    <Badge variant={statusVariant(invitee.rewardStatus)}>
                      {claimStatusLabel(invitee.rewardStatus)}
                    </Badge>

                    {invitee.rewardStatus === "WAITING_FOR_OTHER_COUNTERPARTY" && (
  <p className="max-w-xs text-xs text-muted-foreground">
    At least one qualifying Direct Barter session must be completed
    with someone other than the person who invited this friend.
  </p>
)}
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Reward claim history</CardTitle>
          <CardDescription>
            Pending rewards are not part of the confirmed wallet balance until
            blockchain confirmation and ledger credit complete.
          </CardDescription>
        </CardHeader>

        <CardContent>
          {overview.recentClaims.length === 0 ? (
            <p className="py-4 text-sm text-muted-foreground">
              No reward claims have been reserved yet.
            </p>
          ) : (
            <div className="space-y-3">
              {overview.recentClaims.map((claim) => (
                <div
                  key={claim.claimId}
                  className="flex flex-col gap-2 rounded-lg border p-3 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div>
                    <p className="font-medium">{claim.sourceReference}</p>
                    <p className="text-sm text-muted-foreground">
                      {claim.amountTokens} BLT · {formatDate(claim.createdAt)}
                    </p>

                    {claim.transactionHash && (
                      <p className="break-all font-mono text-xs text-muted-foreground">
                        Tx: {claim.transactionHash}
                      </p>
                    )}
                  </div>

                  <Badge variant={statusVariant(claim.status)}>
                    {claimStatusLabel(claim.status)}
                  </Badge>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

export default ReferralPage;