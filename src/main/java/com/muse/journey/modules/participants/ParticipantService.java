package com.muse.journey.modules.participants;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.muse.journey.modules.trips.Trip;
import com.muse.journey.modules.trips.TripRepository;

/**
 * Public API of the participants slice. The trips slice invites people
 * exclusively through {@link #registerParticipantsToEvent} and
 * {@link #registerParticipantToEvent}.
 */
@Service
public class ParticipantService {

    @Autowired
    private ParticipantRepository repository;

    @Autowired
    private TripRepository tripRepository;

    public List<Participant> registerParticipantsToEvent(List<String> participantsToInvite, Trip trip) {
        List<Participant> participants = participantsToInvite.stream()
                .map(email -> new Participant(email, trip))
                .toList();
        return this.repository.saveAll(participants);
    }

    public ParticipantCreateResponse registerParticipantToEvent(String email, Trip trip) {
        Participant newParticipant = new Participant(email, trip);
        this.repository.save(newParticipant);

        return new ParticipantCreateResponse(newParticipant.getId());
    }

    public void triggerConfirmationEmailToParticipants(UUID tripId) {
    }

    public void triggerConfirmationEmailToParticipant(String email) {
    }

    public List<ParticipantData> getAllParticipantsFromEvent(UUID tripId) {
        return this.repository.findByTripId(tripId)
                .stream()
                .map(ParticipantData::from)
                .toList();
    }

    public Participant createParticipant(ParticipantRequestPayload payload) {
        if (payload.name() == null || payload.name().isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (payload.email() == null || payload.email().isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        Trip trip = null;
        if (payload.trip_id() != null) {
            trip = tripRepository.findById(payload.trip_id())
                    .orElseThrow(() -> new IllegalArgumentException("trip not found"));
        }
        Participant participant = new Participant();
        participant.setName(payload.name());
        participant.setEmail(payload.email());
        participant.setIsConfirmed(false);
        participant.setTrip(trip);
        return this.repository.save(participant);
    }

    public Participant getParticipantById(UUID id) {
        return this.repository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Participant not found"));
    }

    public Participant updateParticipant(UUID id, ParticipantRequestPayload payload) {
        Participant participant = this.repository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Participant not found"));
        if (payload.name() != null && !payload.name().isBlank()) {
            participant.setName(payload.name());
        }
        if (payload.email() != null && !payload.email().isBlank()) {
            participant.setEmail(payload.email());
        }
        return this.repository.save(participant);
    }

    public void deleteParticipant(UUID id) {
        if (!this.repository.existsById(id)) {
            throw new jakarta.persistence.EntityNotFoundException("Participant not found");
        }
        this.repository.deleteById(id);
    }
}
