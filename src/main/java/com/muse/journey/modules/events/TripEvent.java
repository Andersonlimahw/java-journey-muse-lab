package com.muse.journey.modules.events;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.muse.journey.modules.trips.Trip;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vertical slice "events": owns the TripEvent aggregate and the
 * {@code /trips/{id}/events} endpoints.
 */
@Entity
@Table(name = "trip_events")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TripEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1024)
    private String description;

    private String location;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at", nullable = false)
    private LocalDateTime endsAt;

    @ManyToOne
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    public TripEvent(String title, String description, String location, String startsAt, String endsAt, Trip trip) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.startsAt = LocalDateTime.parse(startsAt, DateTimeFormatter.ISO_DATE_TIME);
        this.endsAt = LocalDateTime.parse(endsAt, DateTimeFormatter.ISO_DATE_TIME);
        this.trip = trip;
    }
}
