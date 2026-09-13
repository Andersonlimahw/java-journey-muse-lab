package com.muse.journey.modules.events;

import com.muse.journey.modules.trips.Trip;
import com.muse.journey.modules.trips.TripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Vertical slice "events": every endpoint is trip-scoped, so each handler
 * first loads the trip and then delegates to {@link TripEventService}.
 */
@RestController
@RequestMapping("/trips/{id}/events")
public class TripEventController {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private TripEventService tripEventService;

    @PostMapping
    public ResponseEntity<TripEventResponse> registerEvent(@PathVariable UUID id, @RequestBody TripEventRequestPayload payload) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            TripEventService.validatePayload(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        TripEventResponse eventResponse = this.tripEventService.registerEvent(payload, trip.get());
        return ResponseEntity.ok(eventResponse);
    }

    @GetMapping
    public ResponseEntity<List<TripEventData>> getAllEvents(@PathVariable UUID id) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<TripEventData> eventDataList = this.tripEventService.getAllEventsFromId(id);
        return ResponseEntity.ok(eventDataList);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<TripEventData> getEvent(@PathVariable UUID id, @PathVariable UUID eventId) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return this.tripEventService.getEvent(eventId, trip.get())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<TripEventData> updateEvent(@PathVariable UUID id, @PathVariable UUID eventId, @RequestBody TripEventRequestPayload payload) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            TripEventService.validatePayload(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return this.tripEventService.updateEvent(eventId, payload, trip.get())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> deleteEvent(@PathVariable UUID id, @PathVariable UUID eventId) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (this.tripEventService.deleteEvent(eventId, trip.get())) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
