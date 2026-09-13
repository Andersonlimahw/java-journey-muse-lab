package com.muse.journey.modules.activities;

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
public class ActivityService {

    @Autowired
    private ActivityRepository repository;

    public ActivityResponse registerActivity(ActivityRequestPayload payload, Trip trip) {
        Activity newActivity = new Activity(payload.title(), payload.occurs_at(), trip);
        this.repository.save(newActivity);
        return new ActivityResponse(newActivity.getId());
    }

    public List<ActivityData> getAllActivitiesFromId(UUID tripId) {
        return this.repository.findByTripId(tripId).stream()
                .map(activity -> new ActivityData(activity.getId(), activity.getTitle(), activity.getOccursAt()))
                .toList();
    }

    public Optional<ActivityData> getActivity(UUID activityId, Trip trip) {
        return repository.findByIdAndTrip(activityId, trip)
                .map(a -> new ActivityData(a.getId(), a.getTitle(), a.getOccursAt()));
    }

    public Optional<ActivityData> updateActivity(UUID activityId, ActivityRequestPayload payload, Trip trip) {
        return repository.findByIdAndTrip(activityId, trip)
                .map(activity -> {
                    activity.setTitle(payload.title());
                    activity.setOccursAt(LocalDateTime.parse(payload.occurs_at(), DateTimeFormatter.ISO_DATE_TIME));
                    repository.save(activity);
                    return new ActivityData(activity.getId(), activity.getTitle(), activity.getOccursAt());
                });
    }

    public boolean deleteActivity(UUID activityId, Trip trip) {
        Optional<Activity> activity = repository.findByIdAndTrip(activityId, trip);
        if (activity.isPresent()) {
            repository.delete(activity.get());
            return true;
        }
        return false;
    }

    public static void validatePayload(ActivityRequestPayload payload) {
        if (payload.title() == null || payload.title().isBlank()) {
            throw new IllegalArgumentException("title is required");
        }
        if (payload.occurs_at() == null || payload.occurs_at().isBlank()) {
            throw new IllegalArgumentException("occurs_at is required");
        }
        try {
            LocalDateTime.parse(payload.occurs_at(), DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("occurs_at has invalid format");
        }
    }
}
