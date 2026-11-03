package com.barterlink.ratings.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateRatingRequestDto {

    @NotNull
    private UUID sessionId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer stars;

    private String review;
}