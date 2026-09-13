package com.muse.journey.modules.activities;

import com.muse.journey.modules.trips.Trip;
import com.muse.journey.modules.trips.TripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Vertical slice "activities": every endpoint is trip-scoped, so each handler
 * first loads the trip and then delegates to {@link ActivityService}.
 */
@RestController
@RequestMapping("/trips/{id}/activities")
public class ActivityController {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private ActivityService activityService;

    @PostMapping
    public ResponseEntity<ActivityResponse> registerActivity(@PathVariable UUID id, @RequestBody ActivityRequestPayload payload) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            ActivityService.validatePayload(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        ActivityResponse activityResponse = this.activityService.registerActivity(payload, trip.get());
        return ResponseEntity.ok(activityResponse);
    }

    @GetMapping
    public ResponseEntity<List<ActivityData>> getAllActivities(@PathVariable UUID id) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<ActivityData> activityDataList = this.activityService.getAllActivitiesFromId(id);
        return ResponseEntity.ok(activityDataList);
    }

    @GetMapping("/{activityId}")
    public ResponseEntity<ActivityData> getActivity(@PathVariable UUID id, @PathVariable UUID activityId) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return this.activityService.getActivity(activityId, trip.get())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{activityId}")
    public ResponseEntity<ActivityData> updateActivity(@PathVariable UUID id, @PathVariable UUID activityId, @RequestBody ActivityRequestPayload payload) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        try {
            ActivityService.validatePayload(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        return this.activityService.updateActivity(activityId, payload, trip.get())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{activityId}")
    public ResponseEntity<Void> deleteActivity(@PathVariable UUID id, @PathVariable UUID activityId) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (this.activityService.deleteActivity(activityId, trip.get())) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
