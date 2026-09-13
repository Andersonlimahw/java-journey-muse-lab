package com.muse.journey.modules.events;

import com.muse.journey.modules.trips.Trip;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TripEventService {

    @Autowired
    private TripEventRepository repository;

    public TripEventResponse registerEvent(TripEventRequestPayload payload, Trip trip) {
        TripEvent newEvent = new TripEvent(
                payload.title(),
                payload.description(),
                payload.location(),
                payload.starts_at(),
                payload.ends_at(),
                trip);
        this.repository.save(newEvent);
        return new TripEventResponse(newEvent.getId());
    }

    public List<TripEventData> getAllEventsFromId(UUID tripId) {
        return this.repository.findByTripId(tripId).stream()
                .map(TripEventService::toData)
                .toList();
    }

    public Optional<TripEventData> getEvent(UUID eventId, Trip trip) {
        return repository.findByIdAndTrip(eventId, trip)
                .map(TripEventService::toData);
    }

    public Optional<TripEventData> updateEvent(UUID eventId, TripEventRequestPayload payload, Trip trip) {
        return repository.findByIdAndTrip(eventId, trip)
                .map(event -> {
                    event.setTitle(payload.title());
                    event.setDescription(payload.description());
                    event.setLocation(payload.location());
                    event.setStartsAt(LocalDateTime.parse(payload.starts_at(), DateTimeFormatter.ISO_DATE_TIME));
                    event.setEndsAt(LocalDateTime.parse(payload.ends_at(), DateTimeFormatter.ISO_DATE_TIME));
                    repository.save(event);
                    return toData(event);
                });
    }

    public boolean deleteEvent(UUID eventId, Trip trip) {
        Optional<TripEvent> event = repository.findByIdAndTrip(eventId, trip);
        if (event.isPresent()) {
            repository.delete(event.get());
            return true;
        }
        return false;
    }

    public static void validatePayload(TripEventRequestPayload payload) {
        if (payload.title() == null || payload.title().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        LocalDateTime startsAt = parseDateTime(payload.starts_at(), "starts_at");
        LocalDateTime endsAt = parseDateTime(payload.ends_at(), "ends_at");
        if (endsAt.isBefore(startsAt)) {
            throw new IllegalArgumentException("ends_at must not be before starts_at");
        }
    }

    private static LocalDateTime parseDateTime(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        try {
            return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(field + " has invalid format");
        }
    }

    private static TripEventData toData(TripEvent event) {
        return new TripEventData(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getLocation(),
                event.getStartsAt(),
                event.getEndsAt());
    }
}
