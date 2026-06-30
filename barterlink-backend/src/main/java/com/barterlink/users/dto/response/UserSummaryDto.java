package com.barterlink.users.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSummaryDto {

    private UUID id;

    private String name;

    private String image;

    private Double reputationScore;
}
