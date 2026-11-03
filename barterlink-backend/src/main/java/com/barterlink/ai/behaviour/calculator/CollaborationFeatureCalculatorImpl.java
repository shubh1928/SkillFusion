package com.barterlink.ai.behaviour.calculator;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.agreements.repository.AgreementRepository;
import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.sessions.enums.SessionStatus;
import com.barterlink.sessions.repository.ServiceSessionRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CollaborationFeatureCalculatorImpl
        implements CollaborationFeatureCalculator {

    private final UserRepository userRepository;

    private final AgreementRepository agreementRepository;

    private final ServiceSessionRepository serviceSessionRepository;

    @Override
    public Map<String, Double> calculate(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        Map<String, Double> features = new HashMap<>();

        long collaborationCount =
                agreementRepository.countByRequester(user);

        long completedCollaborationCount =
                agreementRepository.countByRequesterAndStatus(
                        user,
                        AgreementStatus.COMPLETED
                );

        long sessionCount =
                serviceSessionRepository.countByAgreementRequesterId(
                        userId
                );

        long completedSessionCount =
                serviceSessionRepository
                        .countByAgreementRequesterIdAndStatus(
                                userId,
                                SessionStatus.COMPLETED
                        );

        features.put(
                "collaboration_count",
                (double) collaborationCount
        );

        features.put(
                "completed_collaboration_count",
                (double) completedCollaborationCount
        );

        double collaborationCompletionRate = 0.0;

        if (collaborationCount > 0) {

            collaborationCompletionRate =
                    (double) completedCollaborationCount
                            / collaborationCount;
        }

        features.put(
                "collaboration_completion_rate",
                collaborationCompletionRate
        );

        features.put(
                "session_count",
                (double) sessionCount
        );

        double completedSessionRate = 0.0;

        if (sessionCount > 0) {

            completedSessionRate =
                    (double) completedSessionCount
                            / sessionCount;
        }

        features.put(
                "completed_session_rate",
                completedSessionRate
        );

        return features;
    }

}