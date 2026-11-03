package com.barterlink.ai.fraud.repository;

import com.barterlink.ai.fraud.entity.UserFraudAssessment;
import com.barterlink.auth.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserFraudAssessmentRepository
        extends JpaRepository<UserFraudAssessment, UUID> {

    Optional<UserFraudAssessment> findByUser(User user);

}