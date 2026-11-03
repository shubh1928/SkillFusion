package com.barterlink.rewards.repository;

import com.barterlink.rewards.entity.RewardStreakDay;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RewardStreakDayRepository
        extends JpaRepository<RewardStreakDay, UUID> {

    Optional<RewardStreakDay> findByUser_IdAndActivityDateUtc(
            UUID userId,
            LocalDate activityDateUtc
    );

    List<RewardStreakDay>
    findByUser_IdAndActivityDateUtcLessThanEqualAndDailyQualifiedTrueOrderByActivityDateUtcDesc(
            UUID userId,
            LocalDate activityDateUtc
    );

    List<RewardStreakDay>
    findByUser_IdAndActivityDateUtcBetweenOrderByActivityDateUtcAsc(
            UUID userId,
            LocalDate fromDate,
            LocalDate toDate
    );
}