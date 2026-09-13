package com.muse.journey.modules.participants;

import java.util.UUID;

public record ParticipantRequestPayload(String name, String email, UUID trip_id) {
}
