package com.barterlink.ratings.repository;

import com.barterlink.auth.entities.User;
import com.barterlink.ratings.entity.Rating;
import com.barterlink.sessions.entity.ServiceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RatingRepository
        extends JpaRepository<Rating, UUID> {

    List<Rating> findByRatedUser(
            User ratedUser);

    List<Rating> findByRatedBy(
            User ratedBy);

    List<Rating> findBySession(
            ServiceSession session);

    Optional<Rating> findBySessionAndRatedBy(
            ServiceSession session,
            User ratedBy);

    long countByRatedUser(User ratedUser);

    long countByRatedUserAndStarsLessThanEqual(
            User ratedUser,
            Integer stars
    );

    long countByRatedUserAndStarsGreaterThanEqual(
            User ratedUser,
            Integer stars
    );

    @Query("""
       SELECT COALESCE(AVG(r.stars),0)
       FROM Rating r
       WHERE r.ratedUser = :user
       """)
    Double getAverageRating(
            @Param("user") User user);
}