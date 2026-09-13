package com.muse.journey.modules.links;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.muse.journey.modules.trips.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkRepository extends JpaRepository<Link, UUID> {
    List<Link> findByTripId(UUID tripId);
    Optional<Link> findByIdAndTrip(UUID id, Trip trip);
}
