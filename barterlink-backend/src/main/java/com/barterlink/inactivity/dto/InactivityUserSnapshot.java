
package com.barterlink.inactivity.dto;

import java.time.Instant;
import java.util.UUID;

public record InactivityUserSnapshot(
        UUID id,
        String email,
        String name,
        Instant lastActiveAt
) {
}
