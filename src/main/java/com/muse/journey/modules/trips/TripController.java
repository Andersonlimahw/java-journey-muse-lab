package com.muse.journey.modules.trips;

import com.muse.journey.modules.participants.ParticipantCreateResponse;
import com.muse.journey.modules.participants.ParticipantData;
import com.muse.journey.modules.participants.ParticipantRequestPayload;
import com.muse.journey.modules.participants.ParticipantService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Vertical slice "trips": thin HTTP adapter. Business rules live in
 * {@link TripService}; participant side effects go through the participants
 * slice public API ({@link ParticipantService}).
 */
@RestController
@RequestMapping("/trips")
public class TripController {

    @Autowired
    private TripService tripService;

    @Autowired
    private ParticipantService participantService;

    @PostMapping
    public ResponseEntity<TripCreateResponse> createTrip(@RequestBody TripRequestPayload payload) {
        Trip trip = this.tripService.createTrip(payload);
        List<String> invites = payload.emails_to_invite() == null ? List.of() : payload.emails_to_invite();
        List<ParticipantData> participants = this.participantService
                .registerParticipantsToEvent(invites, trip)
                .stream()
                .map(ParticipantData::from)
                .toList();
        return ResponseEntity.ok(new TripCreateResponse(trip.getId(), participants));
    }

    @GetMapping()
    public ResponseEntity<List<Trip>> list() {
        return ResponseEntity.ok(this.tripService.listTrips());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Trip> getById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(this.tripService.getTrip(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/overview")
    public ResponseEntity<TripOverviewResponse> getOverview(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(this.tripService.getOverview(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Trip> update(@PathVariable UUID id, @RequestBody TripRequestPayload payload) {
        try {
            return ResponseEntity.ok(this.tripService.updateTrip(id, payload));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PatchMapping("/{id}/confirmation")
    public ResponseEntity<Trip> getConfirmation(@PathVariable UUID id) {
        try {
            Trip tripUpdated = this.tripService.confirmTrip(id);
            this.participantService.triggerConfirmationEmailToParticipants(id);
            return ResponseEntity.ok(tripUpdated);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/invite")
    public ResponseEntity<ParticipantCreateResponse> invite(@PathVariable UUID id, @RequestBody ParticipantRequestPayload payload) {
        try {
            Trip rawTrip = this.tripService.getTrip(id);
            ParticipantCreateResponse participantId = this.participantService.registerParticipantToEvent(payload.email(), rawTrip);
            if (rawTrip.isConfirmed()) {
                this.participantService.triggerConfirmationEmailToParticipant(payload.email());
            }
            return ResponseEntity.ok(participantId);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}/participants")
    public ResponseEntity<List<ParticipantData>> getParticipants(@PathVariable UUID id) {
        List<ParticipantData> participants = this.participantService.getAllParticipantsFromEvent(id);
        return ResponseEntity.ok(participants);
    }
}
