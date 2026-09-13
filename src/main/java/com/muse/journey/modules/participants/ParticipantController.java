package com.muse.journey.modules.participants;

import com.muse.journey.modules.trips.Trip;
import com.muse.journey.modules.trips.TripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Vertical slice "participants": standalone {@code /participants} resource.
 * Trip-scoped invitation endpoints live in the trips slice and delegate to
 * {@link ParticipantService}.
 */
@RestController
@RequestMapping("/participants")
public class ParticipantController {
    @Autowired
    private ParticipantRepository repository;

    @Autowired
    private TripRepository tripRepository;

    @GetMapping("")
    public ResponseEntity<List<Participant>> get() {
        List<Participant> participants = this.repository.findAll();
        return ResponseEntity.ok(participants);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ParticipantRequestPayload payload) {
        if (payload.email() == null || payload.email().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email is required"));
        }
        if (payload.trip_id() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "trip_id is required"));
        }
        Optional<Trip> trip = tripRepository.findById(payload.trip_id());
        if (trip.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "trip not found"));
        }
        Participant participant = new Participant(payload.email(), trip.get());
        if (payload.name() != null && !payload.name().isBlank()) {
            participant.setName(payload.name());
        }
        repository.save(participant);
        return ResponseEntity.ok(new ParticipantCreateResponse(participant.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Participant> getById(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody ParticipantRequestPayload payload) {
        if (payload.email() == null || payload.email().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email is required"));
        }
        Optional<Participant> existing = repository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Participant participant = existing.get();
        participant.setEmail(payload.email());
        if (payload.name() != null) {
            participant.setName(payload.name());
        }
        repository.save(participant);
        return ResponseEntity.ok(participant);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<Participant> confirmParticipant(@PathVariable UUID id, @RequestBody ParticipantRequestPayload payload) {
        Optional<Participant> participant = this.repository.findById(id);
        if (participant.isPresent()) {
            Participant participantToConfirm = participant.get();
            participantToConfirm.setIsConfirmed(true);
            participantToConfirm.setName(payload.name());

            this.repository.save(participantToConfirm);

            return ResponseEntity.ok(participantToConfirm);
        }
        return ResponseEntity.notFound().build();
    }
}
