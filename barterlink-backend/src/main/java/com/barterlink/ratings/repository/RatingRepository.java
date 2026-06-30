package com.barterlink.ratings.repository;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.auth.entities.User;
import com.barterlink.ratings.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RatingRepository
        extends JpaRepository<Rating, UUID> {

    Optional<Rating> findByAgreement(
            ServiceAgreement agreement);

    List<Rating> findByProvider(
            User provider);

    boolean existsByAgreement(
            ServiceAgreement agreement);
}