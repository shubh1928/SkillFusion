package com.barterlink.rewards.dto.response;

import com.barterlink.rewards.enums.ReferralAttributionStatus;
import com.barterlink.rewards.enums.RewardCampaign;
import com.barterlink.rewards.enums.RewardClaimStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record RewardsOverviewResponseDto(
        Instant generatedAt,
        boolean payoutsEnabled,
        DirectBarterProgress directBarter,
        DailyStreakProgress dailyStreak,
        ReferralProgress referrals,
        List<RewardClaimItem> recentClaims
) {

    public record DirectBarterProgress(
            int rewardPerSession,
            int lifetimeLimit,
            long slotsReserved,
            long slotsRemaining,
            long confirmedClaims,
            long pendingClaims,
            long reconciliationRequired,
            long reviewRequired,
            long failedClaims
    ) {}

    public record DailyStreakProgress(
            int rewardAmount,
            int requiredDays,
            int currentStreakDays,
            boolean loginRecordedToday,
            boolean qualifyingActivityRecordedToday,
            boolean rewardClaimReserved,
            String rewardStatus,
            List<StreakDay> calendar
    ) {}

    public record StreakDay(
            LocalDate date,
            boolean loginRecorded,
            boolean qualifyingActivityRecorded,
            boolean qualified
    ) {}

    public record ReferralProgress(
            String referralCode,
            int rewardPerFriend,
            int lifetimeLimit,
            long rewardSlotsReserved,
            long slotsRemaining,
            long confirmedRewards,
            long pendingRewards,
            long waitingForOtherCounterparty,
            long failedRewards,
            List<ReferralInvitee> invitees
    ) {}

    public record ReferralInvitee(
            String maskedLabel,
            Instant attributedAt,
            int qualifyingSessions,
            ReferralAttributionStatus attributionStatus,
            String rewardStatus
    ) {}

    public record RewardClaimItem(
            UUID claimId,
            RewardCampaign campaign,
            int amountTokens,
            RewardClaimStatus status,
            Instant createdAt,
            Instant confirmedAt,
            String sourceReference,
            String transactionHash
    ) {}
}