package com.muse.journey.modules.participants;

import java.util.UUID;

public record ParticipantData(UUID id, String name, String email, Boolean isConfirmed) {

    public static ParticipantData from(Participant participant) {
        return new ParticipantData(
                participant.getId(),
                participant.getName(),
                participant.getEmail(),
                participant.getIsConfirmed());
    }
}
