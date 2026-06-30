package com.barterlink.users.dto.response;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponseDto {

    private UUID id;

    private String name;

    private String email;

    private String image;

    private String bio;

    private String provider;

    private Double reputationScore;

    private Integer completedServices;

    private Integer totalRatings;

    private Instant createdAt;
}
