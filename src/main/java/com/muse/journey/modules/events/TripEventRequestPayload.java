package com.muse.journey.modules.events;

public record TripEventRequestPayload(
        String title,
        String description,
        String location,
        String starts_at,
        String ends_at) {
}
