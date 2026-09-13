package com.muse.journey.modules.trips;

import java.time.LocalDateTime;
import java.util.UUID;

public record TripOverviewResponse(
        UUID id,
        String destination,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean confirmed,
        String ownerName,
        String ownerEmail
) {
}
