package com.muse.journey.modules.trips;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Vertical slice "trips": all trip write-model rules live here. Cross-slice
 * side effects (inviting participants) go through the participants slice
 * public API, never through its repository.
 */
@Service
public class TripService {

    @Autowired
    private TripRepository repository;

    public Trip createTrip(TripRequestPayload payload) {
        Trip trip = new Trip(payload);
        return this.repository.save(trip);
    }

    public List<Trip> listTrips() {
        return this.repository.findAll();
    }

    public Trip getTrip(UUID id) {
        return this.repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trip not found"));
    }

    public TripOverviewResponse getOverview(UUID id) {
        Trip trip = getTrip(id);
        return new TripOverviewResponse(
                trip.getId(),
                trip.getDestination(),
                trip.getStartsAt(),
                trip.getEndsAt(),
                trip.isConfirmed(),
                trip.getOwnerName(),
                trip.getOwnerEmail());
    }

    public Trip updateTrip(UUID id, TripRequestPayload payload) {
        Trip trip = getTrip(id);
        trip.setStartsAt(LocalDateTime.parse(payload.starts_at(), DateTimeFormatter.ISO_DATE_TIME));
        trip.setEndsAt(LocalDateTime.parse(payload.ends_at(), DateTimeFormatter.ISO_DATE_TIME));
        trip.setDestination(payload.destination());
        return this.repository.save(trip);
    }

    public Trip confirmTrip(UUID id) {
        Trip trip = getTrip(id);
        trip.setConfirmed(true);
        return this.repository.save(trip);
    }
}
