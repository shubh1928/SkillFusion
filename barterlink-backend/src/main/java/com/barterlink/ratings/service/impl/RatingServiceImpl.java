package com.barterlink.ratings.service.impl;

import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.ratings.dto.request.CreateRatingRequestDto;
import com.barterlink.ratings.dto.response.RatingResponseDto;
import com.barterlink.ratings.entity.Rating;
import com.barterlink.ratings.enums.RatingType;
import com.barterlink.ratings.repository.RatingRepository;
import com.barterlink.ratings.service.RatingService;
import com.barterlink.reputation.service.ReputationService;
import com.barterlink.sessions.entity.ServiceSession;
import com.barterlink.sessions.enums.SessionStatus;
import com.barterlink.sessions.repository.ServiceSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.barterlink.notifications.enums.NotificationType;
import com.barterlink.notifications.service.NotificationService;
import com.barterlink.ai.service.TrustAssessmentService;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RatingServiceImpl
        implements RatingService {

    private final RatingRepository ratingRepository;
    private final ServiceSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ReputationService reputationService;
    private final NotificationService notificationService;
    private final TrustAssessmentService trustAssessmentService;

    @Override
    public RatingResponseDto submitRating(
            UUID currentUserId,
            CreateRatingRequestDto request) {

        ServiceSession session =
                sessionRepository.findById(
                                request.getSessionId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Session not found"));

        if (session.getStatus()
                != SessionStatus.COMPLETED) {

            throw new BusinessException(
                    "Session is not completed");
        }

        User currentUser =
                userRepository.findById(currentUserId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        UUID requesterId =
                session.getAgreement()
                        .getRequester()
                        .getId();

        UUID providerId =
                session.getAgreement()
                        .getProvider()
                        .getId();

        if (!requesterId.equals(currentUserId)
                && !providerId.equals(currentUserId)) {

            throw new BusinessException(
                    "You are not part of this session");
        }

        if (ratingRepository.findBySessionAndRatedBy(
                session,
                currentUser).isPresent()) {

            throw new BusinessException(
                    "You have already submitted a rating.");
        }

        User ratedUser;
        RatingType ratingType;

        if (requesterId.equals(currentUserId)) {

            ratedUser =
                    session.getAgreement()
                            .getProvider();

            ratingType =
                    RatingType.REQUESTER_TO_PROVIDER;

        } else {

            ratedUser =
                    session.getAgreement()
                            .getRequester();

            ratingType =
                    RatingType.PROVIDER_TO_REQUESTER;
        }

        Rating rating =
                Rating.builder()
                        .session(session)
                        .ratedBy(currentUser)
                        .ratedUser(ratedUser)
                        .ratingType(ratingType)
                        .stars(request.getStars())
                        .review(request.getReview())
                        .build();

        Rating savedRating =
                ratingRepository.save(rating);

        reputationService.recalculateReputation(
                ratedUser.getId());

        try {

            trustAssessmentService.refreshAssessment(
                    ratedUser.getId());

        } catch (Exception ex) {

            ex.printStackTrace();
        }

        notificationService.createNotification(
                ratedUser.getId(),
                NotificationType.RATING,
                "New Rating Received",
                currentUser.getName()
                        + " rated your completed service.");

        return mapToResponse(savedRating);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingResponseDto> getUserRatings(
            UUID userId) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        return ratingRepository.findByRatedUser(user)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Double calculateReputation(
            UUID userId) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        List<Rating> ratings =
                ratingRepository.findByRatedUser(user);

        if (ratings.isEmpty()) {
            return 0.0;
        }

        return ratings.stream()
                .mapToInt(Rating::getStars)
                .average()
                .orElse(0.0);
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean hasCurrentUserRated(
            UUID currentUserId,
            UUID sessionId
    ) {

        ServiceSession session =
                sessionRepository.findById(sessionId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Session not found"));

        User currentUser =
                userRepository.findById(currentUserId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"));

        return ratingRepository
                .findBySessionAndRatedBy(
                        session,
                        currentUser
                )
                .isPresent();
    }

    private RatingResponseDto mapToResponse(
            Rating rating) {

        RatingResponseDto dto =
                new RatingResponseDto();

        dto.setRatingId(
                rating.getId());

        dto.setSessionId(
                rating.getSession().getId());

        dto.setRatedById(
                rating.getRatedBy().getId());

        dto.setRatedByName(
                rating.getRatedBy().getName());

        dto.setRatedUserId(
                rating.getRatedUser().getId());

        dto.setRatedUserName(
                rating.getRatedUser().getName());

        dto.setRatingType(
                rating.getRatingType());

        dto.setStars(
                rating.getStars());

        dto.setReview(
                rating.getReview());

        dto.setCreatedAt(
                rating.getCreatedAt());

        return dto;
    }

}