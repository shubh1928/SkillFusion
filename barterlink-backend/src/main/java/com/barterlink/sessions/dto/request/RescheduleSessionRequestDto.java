package com.barterlink.sessions.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class RescheduleSessionRequestDto {

    @NotNull(message = "Rescheduled date and time is required")
    @Future(message = "Rescheduled time must be in the future")
    private Instant scheduledAt;
}