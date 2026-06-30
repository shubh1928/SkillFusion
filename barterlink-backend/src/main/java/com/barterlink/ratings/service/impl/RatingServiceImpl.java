package com.barterlink.ratings.service.impl;

import com.barterlink.agreements.entity.ServiceAgreement;
import com.barterlink.agreements.enums.AgreementStatus;
import com.barterlink.agreements.repository.AgreementRepository;
import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.common.exception.BusinessException;
import com.barterlink.common.exception.ResourceNotFoundException;
import com.barterlink.ratings.dto.request.CreateRatingRequestDto;
import com.barterlink.ratings.dto.response.RatingResponseDto;
import com.barterlink.ratings.entity.Rating;
import com.barterlink.ratings.repository.RatingRepository;
import com.barterlink.ratings.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingRepository ratingRepository;
    private final AgreementRepository agreementRepository;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public RatingResponseDto submitRating(
            UUID reviewerId,
            CreateRatingRequestDto request) {

        ServiceAgreement agreement =
                agreementRepository.findById(
                                request.getAgreementId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agreement not found"));

        if (agreement.getStatus()
                != AgreementStatus.COMPLETED) {

            throw new BusinessException(
                    "Agreement is not completed");
        }

        if (ratingRepository.existsByAgreement(
                agreement)) {

            throw new BusinessException(
                    "Rating already submitted");
        }

        User reviewer =
                userRepository.findById(reviewerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Reviewer not found"));

        if (!agreement.getRequester()
                .getId()
                .equals(reviewerId)) {

            throw new BusinessException(
                    "Only requester can submit rating");
        }

        Rating rating =
                Rating.builder()
                        .agreement(agreement)
                        .provider(agreement.getProvider())
                        .reviewer(reviewer)
                        .stars(request.getStars())
                        .review(request.getReview())
                        .build();

        Rating savedRating =
                ratingRepository.save(rating);

        updateProviderReputation(
                agreement.getProvider());

        return mapToResponse(savedRating);
    }

    @Override
    public List<RatingResponseDto> getProviderRatings(
            UUID providerId) {

        User provider =
                userRepository.findById(providerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Provider not found"));

        return ratingRepository.findByProvider(provider)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public Double calculateReputation(
            UUID providerId) {

        User provider =
                userRepository.findById(providerId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Provider not found"));

        List<Rating> ratings =
                ratingRepository.findByProvider(provider);

        if (ratings.isEmpty()) {
            return 0.0;
        }

        return ratings.stream()
                .mapToInt(Rating::getStars)
                .average()
                .orElse(0.0);
    }

    private void updateProviderReputation(
            User provider) {

        List<Rating> ratings =
                ratingRepository.findByProvider(provider);

        double averageRating =
                ratings.stream()
                        .mapToInt(Rating::getStars)
                        .average()
                        .orElse(0.0);

        provider.setReputationScore(
                Math.round(averageRating * 10.0) / 10.0);

        provider.setTotalRatings(
                ratings.size());

        userRepository.save(provider);
    }

    private RatingResponseDto mapToResponse(
            Rating rating) {

        RatingResponseDto dto =
                new RatingResponseDto();

        dto.setRatingId(
                rating.getId());

        dto.setAgreementId(
                rating.getAgreement().getId());

        dto.setProviderId(
                rating.getProvider().getId());

        dto.setProviderName(
                rating.getProvider().getName());

        dto.setReviewerId(
                rating.getReviewer().getId());

        dto.setReviewerName(
                rating.getReviewer().getName());

        dto.setStars(
                rating.getStars());

        dto.setReview(
                rating.getReview());

        dto.setCreatedAt(
                rating.getCreatedAt());

        return dto;
    }
}
