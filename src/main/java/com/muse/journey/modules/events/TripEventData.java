package com.muse.journey.modules.events;

import java.time.LocalDateTime;
import java.util.UUID;

public record TripEventData(
        UUID id,
        String title,
        String description,
        String location,
        LocalDateTime startsAt,
        LocalDateTime endsAt) {
}
