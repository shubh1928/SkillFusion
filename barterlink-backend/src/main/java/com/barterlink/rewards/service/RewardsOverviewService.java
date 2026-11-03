package com.barterlink.rewards.service;

import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.auth.entities.User;
import com.barterlink.blockchain.transaction.BlockchainTransaction;
import com.barterlink.marketplace.enums.ExchangeMode;
import com.barterlink.rewards.dto.response.RewardsOverviewResponseDto;
import com.barterlink.rewards.entity.ReferralAttribution;
import com.barterlink.rewards.entity.RewardClaim;
import com.barterlink.rewards.entity.RewardStreakDay;
import com.barterlink.rewards.enums.ReferralAttributionStatus;
import com.barterlink.rewards.enums.RewardCampaign;
import com.barterlink.rewards.enums.RewardClaimStatus;
import com.barterlink.rewards.repository.ReferralAttributionRepository;
import com.barterlink.rewards.repository.RewardClaimRepository;
import com.barterlink.rewards.repository.RewardStreakDayRepository;
import com.barterlink.sessions.enums.SessionStatus;
import com.barterlink.sessions.repository.ServiceSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RewardsOverviewService {

    private static final List<RewardClaimStatus> PENDING_STATUSES =
            List.of(
                    RewardClaimStatus.PENDING,
                    RewardClaimStatus.PROCESSING,
                    RewardClaimStatus.SUBMITTED
            );

    private static final List<RewardClaimStatus> RESERVED_STATUSES =
            List.of(
                    RewardClaimStatus.PENDING,
                    RewardClaimStatus.PROCESSING,
                    RewardClaimStatus.SUBMITTED,
                    RewardClaimStatus.RECONCILIATION_REQUIRED,
                    RewardClaimStatus.REVIEW_REQUIRED,
                    RewardClaimStatus.CONFIRMED,
                    RewardClaimStatus.FAILED
            );

    private static final String STREAK_SOURCE_TYPE = "STREAK_LIFETIME";
    private static final String STREAK_SOURCE_ID = "LIFETIME";
    private static final String REFERRAL_SOURCE_TYPE = "REFERRAL_QUALIFIED";

    private final RewardClaimRepository rewardClaimRepository;
    private final RewardStreakDayRepository rewardStreakDayRepository;
    private final ReferralAttributionRepository referralAttributionRepository;
    private final ServiceSessionRepository serviceSessionRepository;
    private final ReferralAttributionService referralAttributionService;

    @Value("${barterlink.rewards.scheduler.enabled:false}")
    private boolean payoutsEnabled;

    @Transactional
    public RewardsOverviewResponseDto getOverview(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException(
                    "Authenticated user is required"
            );
        }

        String referralCode =
                referralAttributionService.getOrCreateReferralCode(userId);

        Instant now = Instant.now();
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();

        var directBarter = buildDirectBarterProgress(userId);
        var dailyStreak = buildDailyStreakProgress(userId, today);
        var referrals = buildReferralProgress(userId, referralCode);

        var claims = rewardClaimRepository
                .findTop50ByRecipient_IdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toClaimItem)
                .toList();

        return new RewardsOverviewResponseDto(
                now,
                payoutsEnabled,
                directBarter,
                dailyStreak,
                referrals,
                claims
        );
    }

    private RewardsOverviewResponseDto.DirectBarterProgress
    buildDirectBarterProgress(UUID userId) {

        RewardCampaign campaign =
                RewardCampaign.DIRECT_BARTER_COMPLETION;

        long reserved = rewardClaimRepository
                .countByRecipient_IdAndCampaignAndStatusIn(
                        userId,
                        campaign,
                        RESERVED_STATUSES
                );

        return new RewardsOverviewResponseDto.DirectBarterProgress(
                campaign.getTokenAmount(),
                campaign.getLifetimeLimit(),
                reserved,
                Math.max(0, campaign.getLifetimeLimit() - reserved),
                count(userId, campaign,
                        List.of(RewardClaimStatus.CONFIRMED)),
                count(userId, campaign, PENDING_STATUSES),
                count(userId, campaign,
                        List.of(RewardClaimStatus.RECONCILIATION_REQUIRED)),
                count(userId, campaign,
                        List.of(RewardClaimStatus.REVIEW_REQUIRED)),
                count(userId, campaign,
                        List.of(RewardClaimStatus.FAILED))
        );
    }

    private RewardsOverviewResponseDto.DailyStreakProgress
    buildDailyStreakProgress(UUID userId, LocalDate today) {

        LocalDate start = today.minusDays(29);

        Map<LocalDate, RewardStreakDay> daysByDate = new HashMap<>();

        for (RewardStreakDay day :
                rewardStreakDayRepository
                        .findByUser_IdAndActivityDateUtcBetweenOrderByActivityDateUtcAsc(
                                userId,
                                start,
                                today
                        )) {
            daysByDate.put(day.getActivityDateUtc(), day);
        }

        List<RewardsOverviewResponseDto.StreakDay> calendar =
                new ArrayList<>(30);

        for (int offset = 0; offset < 30; offset++) {
            LocalDate date = start.plusDays(offset);
            RewardStreakDay day = daysByDate.get(date);

            calendar.add(new RewardsOverviewResponseDto.StreakDay(
                    date,
                    day != null && day.isLoginRecorded(),
                    day != null && day.isQualifyingActivityRecorded(),
                    day != null && day.isDailyQualified()
            ));
        }

        RewardStreakDay todayRecord = daysByDate.get(today);

        List<RewardStreakDay> qualifiedDays =
                rewardStreakDayRepository
                        .findByUser_IdAndActivityDateUtcLessThanEqualAndDailyQualifiedTrueOrderByActivityDateUtcDesc(
                                userId,
                                today
                        );

        int currentStreak =
                calculateCurrentStreak(today, qualifiedDays);

        var lifetimeClaim =
                rewardClaimRepository
                        .findByRecipient_IdAndCampaignAndSourceTypeAndSourceId(
                                userId,
                                RewardCampaign.DAILY_ACTIVITY_STREAK_30,
                                STREAK_SOURCE_TYPE,
                                STREAK_SOURCE_ID
                        );

        String status = lifetimeClaim
                .map(claim -> claim.getStatus().name())
                .orElse(
                        currentStreak >= 30
                                ? "DAY_30_REACHED"
                                : "IN_PROGRESS"
                );

        return new RewardsOverviewResponseDto.DailyStreakProgress(
                RewardCampaign.DAILY_ACTIVITY_STREAK_30.getTokenAmount(),
                30,
                Math.min(currentStreak, 30),
                todayRecord != null && todayRecord.isLoginRecorded(),
                todayRecord != null
                        && todayRecord.isQualifyingActivityRecorded(),
                lifetimeClaim.isPresent(),
                status,
                calendar
        );
    }

    private int calculateCurrentStreak(
            LocalDate today,
            List<RewardStreakDay> qualifiedDays
    ) {
        if (qualifiedDays.isEmpty()) {
            return 0;
        }

        LocalDate latest =
                qualifiedDays.get(0).getActivityDateUtc();

        LocalDate expected;

        if (latest.equals(today)) {
            expected = today;
        } else if (latest.equals(today.minusDays(1))) {
            /*
             * The current UTC date may still be in progress.
             * Yesterday's completed streak is still current until today
             * ends without qualification.
             */
            expected = today.minusDays(1);
        } else {
            return 0;
        }

        int count = 0;

        for (RewardStreakDay day : qualifiedDays) {
            LocalDate date = day.getActivityDateUtc();

            if (date.equals(expected)) {
                count++;
                expected = expected.minusDays(1);
            } else if (date.isBefore(expected)) {
                break;
            }
        }

        return count;
    }

    private RewardsOverviewResponseDto.ReferralProgress
    buildReferralProgress(UUID userId, String referralCode) {

        RewardCampaign campaign = RewardCampaign.REFERRAL_QUALIFIED;

        long reserved = rewardClaimRepository
                .countByRecipient_IdAndCampaignAndStatusIn(
                        userId,
                        campaign,
                        RESERVED_STATUSES
                );

        List<RewardsOverviewResponseDto.ReferralInvitee> invitees =
                referralAttributionRepository
                        .findByReferrer_IdOrderByAttributedAtDesc(userId)
                        .stream()
                        .map(attribution -> toInvitee(userId, attribution))
                        .toList();

        long waitingForOtherCounterpartyCount = invitees.stream()
                .filter(item ->
                        item.attributionStatus()
                                == ReferralAttributionStatus.REVIEW_REQUIRED
                )
                .count();

        return new RewardsOverviewResponseDto.ReferralProgress(
                referralCode,
                campaign.getTokenAmount(),
                campaign.getLifetimeLimit(),
                reserved,
                Math.max(0, campaign.getLifetimeLimit() - reserved),
                count(userId, campaign,
                        List.of(RewardClaimStatus.CONFIRMED)),
                count(userId, campaign, PENDING_STATUSES),
                waitingForOtherCounterpartyCount,
                count(userId, campaign,
                        List.of(RewardClaimStatus.FAILED)),
                invitees
        );
    }

    private RewardsOverviewResponseDto.ReferralInvitee toInvitee(
            UUID referrerId,
            ReferralAttribution attribution
    ) {
        UUID referredUserId =
                attribution.getReferredUser().getId();

        long actualSessions =
                serviceSessionRepository
                        .countCompletedDirectBarterSessionsForParticipantSince(
                                referredUserId,
                                attribution.getAttributedAt(),
                                SessionStatus.COMPLETED,
                                AgreementStatus.COMPLETED,
                                ExchangeMode.DIRECT_BARTER
                        );

        int qualifyingSessions =
                (int) Math.min(actualSessions, 2);

        var claim =
                rewardClaimRepository
                        .findByRecipient_IdAndCampaignAndSourceTypeAndSourceId(
                                referrerId,
                                RewardCampaign.REFERRAL_QUALIFIED,
                                REFERRAL_SOURCE_TYPE,
                                attribution.getId().toString()
                        );

        String rewardStatus;

        if (attribution.getStatus()
                == ReferralAttributionStatus.LIFETIME_CAP_REACHED) {
            rewardStatus = "LIFETIME_CAP_REACHED";

        } else if (attribution.getStatus()
                == ReferralAttributionStatus.REVIEW_REQUIRED) {
            rewardStatus = "WAITING_FOR_OTHER_COUNTERPARTY";

        } else if (claim.isPresent()) {
            rewardStatus = claim.get().getStatus().name();

        } else if (attribution.getStatus()
                == ReferralAttributionStatus.REWARD_QUEUED) {
            rewardStatus = "PENDING";

        } else if (qualifyingSessions >= 2
                && attribution.getStatus()
                    == ReferralAttributionStatus.QUALIFIED) {
            rewardStatus = "QUALIFIED";

        } else if (qualifyingSessions == 1) {
            rewardStatus = "ONE_OF_TWO";

        } else {
            rewardStatus = "SIGNED_UP";
        }

        return new RewardsOverviewResponseDto.ReferralInvitee(
                maskName(attribution.getReferredUser()),
                attribution.getAttributedAt(),
                qualifyingSessions,
                attribution.getStatus(),
                rewardStatus
        );
    }

    private String maskName(User user) {
        String name = user.getName();

        if (name != null && !name.isBlank()) {
            String trimmed = name.trim();

            return trimmed.substring(0, 1).toUpperCase() + "••••";
        }

        return "Member " + user.getId().toString().substring(0, 6);
    }

    private RewardsOverviewResponseDto.RewardClaimItem toClaimItem(
            RewardClaim claim
    ) {
        String sourceReference = switch (claim.getCampaign()) {
            case DIRECT_BARTER_COMPLETION ->
                    "Direct Barter session " + shorten(claim.getSourceId());

            case DAILY_ACTIVITY_STREAK_30 ->
                    "30-day activity streak";

            case REFERRAL_QUALIFIED ->
                    "Qualified referral";
        };

        BlockchainTransaction tx = claim.getBlockchainTransaction();

        String transactionHash =
                tx == null ? null : tx.getTransactionHash();

        return new RewardsOverviewResponseDto.RewardClaimItem(
                claim.getId(),
                claim.getCampaign(),
                claim.getAmountTokens(),
                claim.getStatus(),
                claim.getCreatedAt(),
                claim.getConfirmedAt(),
                sourceReference,
                transactionHash
        );
    }

    private String shorten(String value) {
        if (value == null || value.length() <= 8) {
            return value;
        }

        return value.substring(0, 8) + "…";
    }

    private long count(
            UUID userId,
            RewardCampaign campaign,
            List<RewardClaimStatus> statuses
    ) {
        return rewardClaimRepository
                .countByRecipient_IdAndCampaignAndStatusIn(
                        userId,
                        campaign,
                        statuses
                );
    }
}