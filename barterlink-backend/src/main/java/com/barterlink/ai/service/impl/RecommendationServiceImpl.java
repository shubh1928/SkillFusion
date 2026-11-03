package com.barterlink.ai.service.impl;

import com.barterlink.ai.client.AIClient;
import com.barterlink.ai.dto.RecommendationRequest;
import com.barterlink.ai.dto.RequestData;
import com.barterlink.ai.service.RecommendationService;
import com.barterlink.auth.entities.User;
import com.barterlink.auth.repositories.UserRepository;
import com.barterlink.marketplace.dto.response.ServiceRequestResponseDto;
import com.barterlink.marketplace.enums.ServiceRequestStatus;
import com.barterlink.marketplace.repository.ServiceRequestRepository;
import com.barterlink.skills.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.barterlink.ai.dto.RecommendedRequest;
import com.barterlink.ai.dto.RecommendationResponse;
import com.barterlink.marketplace.entity.ServiceRequest;
import com.barterlink.marketplace.mapper.MarketplaceMapper;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl
        implements RecommendationService {

    private final UserRepository userRepository;

    private final UserSkillRepository userSkillRepository;

    private final ServiceRequestRepository serviceRequestRepository;

    private final MarketplaceMapper marketplaceMapper;

    private final AIClient aiClient;

    @Override
    public List<ServiceRequestResponseDto> getRecommendations(
            UUID userId
    ) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new RuntimeException("User not found"));

        var userSkills =
                userSkillRepository.findByUser(user);

        System.out.println("User Skills");

        List<String> skills =

                        userSkills.stream()

                                .map(userSkill ->
                                        userSkill.getSkill().getName())

                                .toList();

        var openRequests =
                serviceRequestRepository
                        .findByStatusOrderByCreatedAtDesc(
                                ServiceRequestStatus.OPEN
                        )

                        .stream()

                        .filter(request ->

                                !request.getRequester()
                                        .getId()
                                        .equals(userId)

                        )

                        .toList();

        List<RequestData> requests =

                openRequests.stream()

                        .map(request ->

                                RequestData.builder()

                                        .requestId(
                                                request.getId().toString()
                                        )

                                        .title(
                                                request.getRequestTitle()
                                        )

                                        .description(
                                                request.getRequestDescription()
                                        )

                                        .build()

                        )

                        .toList();

        System.out.println("AI Skills");

        skills.forEach(System.out::println);

        requests.forEach(request -> {

            System.out.println("----------------");

            System.out.println(request.getRequestId());

            System.out.println(request.getTitle());

            System.out.println(request.getDescription());

        });

        RecommendationRequest recommendationRequest =

                RecommendationRequest.builder()

                        .skills(skills)

                        .requests(requests)

                        .build();

        System.out.println("========== Calling AI ==========");

        RecommendationResponse recommendationResponse =
                aiClient.recommend(recommendationRequest);

        System.out.println("========== AI Returned ==========");

        System.out.println("===== AI Recommendations =====");

        recommendationResponse.getRecommendations()
                .forEach(item -> {

                    System.out.println(
                            item.getRequestId()
                    );

                    System.out.println(
                            item.getSimilarityScore()
                    );

                    System.out.println("----------------");
                });

// Build AI ranking map
        System.out.println("========== Building Ranking ==========");

        Map<UUID, Integer> ranking = new HashMap<>();

        int index = 0;

        for (RecommendedRequest recommendation :
                recommendationResponse.getRecommendations()) {

            ranking.put(
                    UUID.fromString(recommendation.getRequestId()),
                    index++
            );
        }

// Fetch matching requests
        List<ServiceRequest> matchedRequests =
                serviceRequestRepository.findByIdIn(
                        ranking.keySet().stream().toList()
                );

// Preserve AI ranking
        matchedRequests.sort(
                Comparator.comparingInt(
                        request -> ranking.get(request.getId())
                )
        );

// Convert to DTO
        System.out.println("========== Returning Response ==========");
        System.out.println("Matched Requests = " + matchedRequests.size());


        return matchedRequests.stream()

                .map(marketplaceMapper::toResponse)

                .collect(Collectors.toList());

    }

}