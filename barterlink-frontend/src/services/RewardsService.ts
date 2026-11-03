import apiClient from "@/config/ApiClient";
import type ApiResponse from "@/models/ApiResponse";

export type RewardCampaign =
  | "DIRECT_BARTER_COMPLETION"
  | "DAILY_ACTIVITY_STREAK_30"
  | "REFERRAL_QUALIFIED";

export type RewardClaimStatus =
  | "PENDING"
  | "PROCESSING"
  | "SUBMITTED"
  | "RECONCILIATION_REQUIRED"
  | "REVIEW_REQUIRED"
  | "CONFIRMED"
  | "FAILED";

export type ReferralAttributionStatus =
  | "TRACKING"
  | "QUALIFIED"
  | "REWARD_QUEUED"
  | "REVIEW_REQUIRED"
  | "LIFETIME_CAP_REACHED";

export interface RewardsOverview {
  generatedAt: string;
  payoutsEnabled: boolean;

  directBarter: {
    rewardPerSession: number;
    lifetimeLimit: number;
    slotsReserved: number;
    slotsRemaining: number;
    confirmedClaims: number;
    pendingClaims: number;
    reconciliationRequired: number;
    reviewRequired: number;
    failedClaims: number;
  };

  dailyStreak: {
    rewardAmount: number;
    requiredDays: number;
    currentStreakDays: number;
    loginRecordedToday: boolean;
    qualifyingActivityRecordedToday: boolean;
    rewardClaimReserved: boolean;
    rewardStatus: string;
    calendar: Array<{
      date: string;
      loginRecorded: boolean;
      qualifyingActivityRecorded: boolean;
      qualified: boolean;
    }>;
  };

  referrals: {
    referralCode: string;
    rewardPerFriend: number;
    lifetimeLimit: number;
    rewardSlotsReserved: number;
    slotsRemaining: number;
    confirmedRewards: number;
    pendingRewards: number;
waitingForOtherCounterparty: number;
failedRewards: number;
    invitees: Array<{
      maskedLabel: string;
      attributedAt: string;
      qualifyingSessions: number;
      attributionStatus: ReferralAttributionStatus;
      rewardStatus: string;
    }>;
  };

  recentClaims: Array<{
    claimId: string;
    campaign: RewardCampaign;
    amountTokens: number;
    status: RewardClaimStatus;
    createdAt: string;
    confirmedAt: string | null;
    sourceReference: string;
    transactionHash: string | null;
  }>;
}

export const getRewardsOverview = async (): Promise<RewardsOverview> => {
  const response = await apiClient.get<ApiResponse<RewardsOverview>>(
    "/rewards/me/overview"
  );

  const overview = response.data?.data;

  if (!overview) {
    throw new Error("The server did not return reward progress.");
  }

  return overview;
};