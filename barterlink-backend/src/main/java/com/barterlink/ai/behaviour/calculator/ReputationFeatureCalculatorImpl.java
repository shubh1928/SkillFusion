package com.barterlink.ai.behaviour.calculator;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.ratings.repository.RatingRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReputationFeatureCalculatorImpl
        implements ReputationFeatureCalculator {

    private final UserRepository userRepository;

    private final RatingRepository ratingRepository;

    @Override
    public Map<String, Double> calculate(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Map<String, Double> features = new HashMap<>();

        long ratingCount =
                ratingRepository.countByRatedUser(user);

        double averageRating =
                ratingRepository.getAverageRating(user);

        long lowRatingCount =
                ratingRepository.countByRatedUserAndStarsLessThanEqual(
                        user,
                        2
                );

        long highRatingCount =
                ratingRepository.countByRatedUserAndStarsGreaterThanEqual(
                        user,
                        4
                );

        features.put(
                "rating_count",
                (double) ratingCount
        );

        features.put(
                "average_rating",
                averageRating
        );

        double lowRatingRatio = 0.0;
        double highRatingRatio = 0.0;
        double ratingConsistency = 0.0;

        if (ratingCount > 0) {

            lowRatingRatio =
                    (double) lowRatingCount / ratingCount;

            highRatingRatio =
                    (double) highRatingCount / ratingCount;

            ratingConsistency =
                    1.0 - lowRatingRatio;
        }

        features.put(
                "low_rating_ratio",
                lowRatingRatio
        );

        features.put(
                "high_rating_ratio",
                highRatingRatio
        );

        features.put(
                "rating_consistency",
                ratingConsistency
        );

        return features;
    }

}