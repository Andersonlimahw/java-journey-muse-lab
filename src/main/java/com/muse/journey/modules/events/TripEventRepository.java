package com.muse.journey.modules.events;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.muse.journey.modules.trips.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripEventRepository extends JpaRepository<TripEvent, UUID> {
    List<TripEvent> findByTripId(UUID tripId);
    Optional<TripEvent> findByIdAndTrip(UUID id, Trip trip);
}
