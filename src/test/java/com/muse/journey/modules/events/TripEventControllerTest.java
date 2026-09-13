package com.muse.journey.modules.events;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TripEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tripId;

    @BeforeEach
    void setUp() throws Exception {
        String tripJson = objectMapper.writeValueAsString(Map.of(
                "destination", "São Paulo",
                "starts_at", "2025-01-01T10:00:00",
                "ends_at", "2025-01-10T10:00:00",
                "owner_name", "John",
                "owner_email", "john@email.com",
                "emails_to_invite", java.util.List.of()
        ));

        var result = mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        tripId = objectMapper.readTree(responseBody).get("uuid").asText();
    }

    private Map<String, Object> validPayload(String title) {
        return Map.of(
                "title", title,
                "description", "Team offsite",
                "location", "São Paulo",
                "starts_at", "2025-01-05T14:00:00",
                "ends_at", "2025-01-05T16:00:00");
    }

    private String createEvent(String title) throws Exception {
        String json = objectMapper.writeValueAsString(validPayload(title));
        var result = mockMvc.perform(post("/trips/{id}/events", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("eventId").asText();
    }

    // --- CREATE ---

    @Test
    void createEvent_ValidInput_Returns200() throws Exception {
        String json = objectMapper.writeValueAsString(validPayload("Kickoff"));

        mockMvc.perform(post("/trips/{id}/events", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").isString());
    }

    @Test
    void createEvent_WithoutOptionalFields_Returns200() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "title", "Kickoff",
                "starts_at", "2025-01-05T14:00:00",
                "ends_at", "2025-01-05T16:00:00"));

        mockMvc.perform(post("/trips/{id}/events", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").isString());
    }

    @Test
    void createEvent_BlankTitle_Returns400() throws Exception {
        Map<String, Object> payload = new java.util.HashMap<>(validPayload(""));
        String json = objectMapper.writeValueAsString(payload);

        mockMvc.perform(post("/trips/{id}/events", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_NullTitle_Returns400() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "starts_at", "2025-01-05T14:00:00",
                "ends_at", "2025-01-05T16:00:00"));

        mockMvc.perform(post("/trips/{id}/events", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_BlankStartsAt_Returns400() throws Exception {
        Map<String, Object> payload = new java.util.HashMap<>(validPayload("Kickoff"));
        payload.put("starts_at", "");
        String json = objectMapper.writeValueAsString(payload);

        mockMvc.perform(post("/trips/{id}/events", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_InvalidEndsAt_Returns400() throws Exception {
        Map<String, Object> payload = new java.util.HashMap<>(validPayload("Kickoff"));
        payload.put("ends_at", "not-a-date");
        String json = objectMapper.writeValueAsString(payload);

        mockMvc.perform(post("/trips/{id}/events", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_EndsBeforeStarts_Returns400() throws Exception {
        Map<String, Object> payload = new java.util.HashMap<>(validPayload("Kickoff"));
        payload.put("starts_at", "2025-01-06T16:00:00");
        payload.put("ends_at", "2025-01-06T14:00:00");
        String json = objectMapper.writeValueAsString(payload);

        mockMvc.perform(post("/trips/{id}/events", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createEvent_NonExistentTrip_Returns404() throws Exception {
        String json = objectMapper.writeValueAsString(validPayload("Kickoff"));

        mockMvc.perform(post("/trips/{id}/events", "00000000-0000-0000-0000-000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    // --- LIST ---

    @Test
    void getEvents_ValidTrip_Returns200() throws Exception {
        mockMvc.perform(get("/trips/{id}/events", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getEvents_NonExistentTrip_Returns404() throws Exception {
        mockMvc.perform(get("/trips/{id}/events", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    // --- GET SINGLE ---

    @Test
    void getEvent_ValidId_Returns200() throws Exception {
        String eventId = createEvent("Beach Day");

        mockMvc.perform(get("/trips/{tripId}/events/{eventId}", tripId, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId))
                .andExpect(jsonPath("$.title").value("Beach Day"))
                .andExpect(jsonPath("$.startsAt").value("2025-01-05T14:00:00"))
                .andExpect(jsonPath("$.endsAt").value("2025-01-05T16:00:00"));
    }

    @Test
    void getEvent_NonExistentEvent_Returns404() throws Exception {
        mockMvc.perform(get("/trips/{tripId}/events/{eventId}", tripId, "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEvent_EventFromAnotherTrip_Returns404() throws Exception {
        String eventId = createEvent("Beach Day");

        String otherTripJson = objectMapper.writeValueAsString(Map.of(
                "destination", "Rio",
                "starts_at", "2025-02-01T10:00:00",
                "ends_at", "2025-02-10T10:00:00",
                "owner_name", "Jane",
                "owner_email", "jane@email.com",
                "emails_to_invite", java.util.List.of()
        ));
        var otherResult = mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(otherTripJson))
                .andExpect(status().isOk())
                .andReturn();
        String otherTripId = objectMapper.readTree(otherResult.getResponse().getContentAsString()).get("uuid").asText();

        mockMvc.perform(get("/trips/{tripId}/events/{eventId}", otherTripId, eventId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEvent_NonExistentTrip_Returns404() throws Exception {
        String eventId = createEvent("Beach Day");

        mockMvc.perform(get("/trips/{tripId}/events/{eventId}", "00000000-0000-0000-0000-000000000000", eventId))
                .andExpect(status().isNotFound());
    }

    // --- UPDATE ---

    @Test
    void updateEvent_ValidInput_Returns200() throws Exception {
        String eventId = createEvent("Old Name");

        String json = objectMapper.writeValueAsString(Map.of(
                "title", "New Name",
                "description", "Updated",
                "location", "Rio",
                "starts_at", "2025-01-07T10:00:00",
                "ends_at", "2025-01-07T12:00:00"));

        mockMvc.perform(put("/trips/{tripId}/events/{eventId}", tripId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Name"))
                .andExpect(jsonPath("$.location").value("Rio"));
    }

    @Test
    void updateEvent_BlankTitle_Returns400() throws Exception {
        String eventId = createEvent("Old");

        Map<String, Object> payload = new java.util.HashMap<>(validPayload(""));
        String json = objectMapper.writeValueAsString(payload);

        mockMvc.perform(put("/trips/{tripId}/events/{eventId}", tripId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEvent_EndsBeforeStarts_Returns400() throws Exception {
        String eventId = createEvent("Old");

        Map<String, Object> payload = new java.util.HashMap<>(validPayload("New"));
        payload.put("starts_at", "2025-01-07T12:00:00");
        payload.put("ends_at", "2025-01-07T10:00:00");
        String json = objectMapper.writeValueAsString(payload);

        mockMvc.perform(put("/trips/{tripId}/events/{eventId}", tripId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateEvent_NonExistentEvent_Returns404() throws Exception {
        String json = objectMapper.writeValueAsString(validPayload("New"));

        mockMvc.perform(put("/trips/{tripId}/events/{eventId}", tripId, "00000000-0000-0000-0000-000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    // --- DELETE ---

    @Test
    void deleteEvent_ValidId_Returns204() throws Exception {
        String eventId = createEvent("To Delete");

        mockMvc.perform(delete("/trips/{tripId}/events/{eventId}", tripId, eventId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/trips/{tripId}/events/{eventId}", tripId, eventId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteEvent_NonExistentEvent_Returns404() throws Exception {
        mockMvc.perform(delete("/trips/{tripId}/events/{eventId}", tripId, "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }
}
