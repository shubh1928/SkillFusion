package com.barterlink.reputation.service.impl;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.ratings.repository.RatingRepository;
import com.barterlink.reputation.service.ReputationService;
import com.barterlink.sessions.repository.ServiceSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.barterlink.sessions.enums.SessionStatus;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReputationServiceImpl
        implements ReputationService {

    private final UserRepository userRepository;
    private final RatingRepository ratingRepository;
    private final ServiceSessionRepository sessionRepository;

    @Override
    public void recalculateReputation(
            UUID userId) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        /*
         * Average Rating
         */
        double averageRating =
                ratingRepository.getAverageRating(user);

        long totalRatings =
                ratingRepository.countByRatedUser(user);

        /*
         * Completed Sessions
         */
        long completedSessions =
                sessionRepository.countByAgreementProviderIdAndStatus(
                        userId,
                        SessionStatus.COMPLETED)
                        +
                        sessionRepository.countByAgreementRequesterIdAndStatus(
                                userId,
                                SessionStatus.COMPLETED);

        /*
         * Disputed Sessions
         */
        long disputedSessions =
                sessionRepository.countByAgreementProviderIdAndStatus(
                        userId,
                        SessionStatus.DISPUTED)
                        +
                        sessionRepository.countByAgreementRequesterIdAndStatus(
                                userId,
                                SessionStatus.DISPUTED);

        /*
         * Total Sessions
         */
        long totalSessions =
                sessionRepository.countByAgreementProviderId(userId)
                        +
                        sessionRepository.countByAgreementRequesterId(userId);

        /*
         * Success Rate
         */
        double successRate = 100.0;

        if (totalSessions > 0) {

            successRate =
                    ((double) completedSessions / totalSessions) * 100.0;
        }

        /*
         * Dispute Penalty
         */
        double disputePenalty = 0;

        if (totalSessions > 0) {

            disputePenalty =
                    ((double) disputedSessions / totalSessions) * 100.0;
        }

        /*
         * Normalize values
         */
        double ratingScore =
                (averageRating / 5.0) * 100.0;

        double completedScore =
                Math.min(completedSessions * 2.0, 100.0);

        /*
         * Weighted Reputation
         */
        double reputationScore =

                ratingScore * 0.50

                        +

                        completedScore * 0.20

                        +

                        successRate * 0.20

                        -

                        disputePenalty * 0.10;

        reputationScore =
                Math.max(0,
                        Math.min(100, reputationScore));

        /*
         * Effective Skill Value
         *
         * Base Skill Unit = 1.0
         */
        double effectiveSkillValue =
                1.0 + (reputationScore / 100.0);

        user.setReputationScore(
                reputationScore);

        user.setEffectiveSkillValue(
                Math.round(effectiveSkillValue * 100.0) / 100.0);

        user.setCompletedServices(
                (int) completedSessions);

        user.setTotalRatings(
                (int) totalRatings);

        userRepository.save(user);
    }

    @Override
    public double calculateEffectiveSkillValue(
            java.util.UUID userId) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        /*
         * Temporary implementation.
         */

        return user.getReputationScore();
    }

}