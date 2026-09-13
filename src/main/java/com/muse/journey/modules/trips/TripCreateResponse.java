package com.muse.journey.modules.trips;

import com.muse.journey.modules.participants.ParticipantData;

import java.util.List;
import java.util.UUID;

/**
 * Kept wire-compatible with the original {@code TripCreateResponse}: the JSON
 * still exposes {@code uuid} plus the invited {@code participants}. The only
 * change is that participants are now DTOs instead of JPA entities, which
 * removes the infinite {@code participant -> trip -> participant} recursion
 * risk and stops leaking internal state.
 */
public record TripCreateResponse(UUID uuid, List<ParticipantData> participants) {
}
