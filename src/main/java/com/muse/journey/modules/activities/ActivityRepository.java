package com.muse.journey.modules.activities;

import com.muse.journey.modules.trips.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    List<Activity> findByTripId(UUID tripId);
    Optional<Activity> findByIdAndTrip(UUID id, Trip trip);
}
