package com.muse.journey.modules.trips;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression tests for the bugs fixed during the modular-monolith port
 * (see docs/adr/0001-modular-monolith-vertical-slices.md).
 */
@SpringBootTest
@AutoConfigureMockMvc
class TripSliceRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // No global cleanup: the shared in-memory DB keeps rows from other test
    // classes, and wiping trips would violate their FKs. Every assertion below
    // is scoped to the trip it creates, so order-independence holds without it.

    private String createTrip(String destination, String... invites) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "destination", destination,
                "starts_at", "2025-01-01T10:00:00",
                "ends_at", "2025-01-10T10:00:00",
                "owner_name", "John",
                "owner_email", "john@email.com",
                "emails_to_invite", java.util.List.of(invites)));
        var result = mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("uuid").asText();
    }

    @Test
    void update_shouldChangeStartsAt_notJustEndsAt() throws Exception {
        String tripId = createTrip("São Paulo");

        String updateJson = objectMapper.writeValueAsString(Map.of(
                "destination", "Rio",
                "starts_at", "2025-02-01T10:00:00",
                "ends_at", "2025-02-10T10:00:00"));

        mockMvc.perform(put("/trips/{id}", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.destination").value("Rio"))
                .andExpect(jsonPath("$.startsAt").value("2025-02-01T10:00:00"))
                .andExpect(jsonPath("$.endsAt").value("2025-02-10T10:00:00"));
    }

    @Test
    void create_shouldReturnOnlyInvitedParticipants() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "destination", "São Paulo",
                "starts_at", "2025-01-01T10:00:00",
                "ends_at", "2025-01-10T10:00:00",
                "owner_name", "John",
                "owner_email", "john@email.com",
                "emails_to_invite", java.util.List.of("a@email.com")));
        var result = mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.participants.length()").value(1))
                .andReturn();
        String tripId = objectMapper.readTree(result.getResponse().getContentAsString()).get("uuid").asText();

        createTrip("Rio", "b@email.com", "c@email.com");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/trips/{id}/participants", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
