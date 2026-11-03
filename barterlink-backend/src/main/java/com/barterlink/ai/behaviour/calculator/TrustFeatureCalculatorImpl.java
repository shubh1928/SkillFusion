package com.barterlink.ai.behaviour.calculator;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.skills.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TrustFeatureCalculatorImpl
        implements TrustFeatureCalculator {

    private final UserRepository userRepository;

    private final UserSkillRepository userSkillRepository;

    @Override
    public Map<String, Double> calculate(
            UUID userId
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Map<String, Double> features = new HashMap<>();

        // -------------------------------
        // Account Maturity (days)
        // -------------------------------
        double accountMaturity =
                Duration.between(
                        user.getCreatedAt(),
                        Instant.now()
                ).toDays();

        // -------------------------------
        // Service Experience
        // -------------------------------
        double serviceExperience =
                user.getCompletedServices().doubleValue();

        // -------------------------------
        // Reputation Index
        // -------------------------------
        double reputationIndex =
                user.getReputationScore();

        // -------------------------------
        // Skill Reliability
        // -------------------------------
        double skillReliability =
                user.getEffectiveSkillValue();

        // -------------------------------
        // Profile Completeness
        // -------------------------------
        int completedFields = 0;

        if (user.getName() != null && !user.getName().isBlank())
            completedFields++;

        if (user.getEmail() != null && !user.getEmail().isBlank())
            completedFields++;

        if (user.getImage() != null && !user.getImage().isBlank())
            completedFields++;

        if (user.getBio() != null && !user.getBio().isBlank())
            completedFields++;

        if (!userSkillRepository.findByUser(user).isEmpty())
            completedFields++;

        double profileCompleteness =
                completedFields / 5.0;

        features.put(
                "account_maturity",
                accountMaturity
        );

        features.put(
                "profile_completeness",
                profileCompleteness
        );

        features.put(
                "service_experience",
                serviceExperience
        );

        features.put(
                "reputation_index",
                reputationIndex
        );

        features.put(
                "skill_reliability",
                skillReliability
        );

        return features;

    }

}