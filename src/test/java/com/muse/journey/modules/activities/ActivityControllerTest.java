package com.muse.journey.modules.activities;

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
class ActivityControllerTest {

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

    // --- CREATE ---

    @Test
    void createActivity_ValidInput_Returns200() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "title", "Visit Museum",
                "occurs_at", "2025-01-05T14:00:00"
        ));

        mockMvc.perform(post("/trips/{id}/activities", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityId").isString());
    }

    @Test
    void createActivity_BlankTitle_Returns400() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "title", "",
                "occurs_at", "2025-01-05T14:00:00"
        ));

        mockMvc.perform(post("/trips/{id}/activities", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createActivity_NullTitle_Returns400() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "occurs_at", "2025-01-05T14:00:00"
        ));

        mockMvc.perform(post("/trips/{id}/activities", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createActivity_BlankOccursAt_Returns400() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "title", "Museum",
                "occurs_at", ""
        ));

        mockMvc.perform(post("/trips/{id}/activities", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createActivity_InvalidOccursAt_Returns400() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "title", "Museum",
                "occurs_at", "not-a-date"
        ));

        mockMvc.perform(post("/trips/{id}/activities", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createActivity_NonExistentTrip_Returns404() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "title", "Museum",
                "occurs_at", "2025-01-05T14:00:00"
        ));

        mockMvc.perform(post("/trips/{id}/activities", "00000000-0000-0000-0000-000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    // --- LIST ---

    @Test
    void getActivities_ValidTrip_Returns200() throws Exception {
        mockMvc.perform(get("/trips/{id}/activities", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getActivities_NonExistentTrip_Returns404() throws Exception {
        mockMvc.perform(get("/trips/{id}/activities", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    // --- GET SINGLE ---

    private String createActivity(String title, String occursAt) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "title", title,
                "occurs_at", occursAt
        ));
        var result = mockMvc.perform(post("/trips/{id}/activities", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("activityId").asText();
    }

    @Test
    void getActivity_ValidId_Returns200() throws Exception {
        String activityId = createActivity("Beach", "2025-01-06T09:00:00");

        mockMvc.perform(get("/trips/{tripId}/activities/{activityId}", tripId, activityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(activityId))
                .andExpect(jsonPath("$.title").value("Beach"));
    }

    @Test
    void getActivity_NonExistentActivity_Returns404() throws Exception {
        mockMvc.perform(get("/trips/{tripId}/activities/{activityId}", tripId, "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getActivity_ActivityFromAnotherTrip_Returns404() throws Exception {
        String activityId = createActivity("Beach", "2025-01-06T09:00:00");

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

        mockMvc.perform(get("/trips/{tripId}/activities/{activityId}", otherTripId, activityId))
                .andExpect(status().isNotFound());
    }

    @Test
    void getActivity_NonExistentTrip_Returns404() throws Exception {
        String activityId = createActivity("Beach", "2025-01-06T09:00:00");

        mockMvc.perform(get("/trips/{tripId}/activities/{activityId}", "00000000-0000-0000-0000-000000000000", activityId))
                .andExpect(status().isNotFound());
    }

    // --- UPDATE ---

    @Test
    void updateActivity_ValidInput_Returns200() throws Exception {
        String activityId = createActivity("Old Name", "2025-01-06T09:00:00");

        String json = objectMapper.writeValueAsString(Map.of(
                "title", "New Name",
                "occurs_at", "2025-01-07T10:00:00"
        ));

        mockMvc.perform(put("/trips/{tripId}/activities/{activityId}", tripId, activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Name"));
    }

    @Test
    void updateActivity_BlankTitle_Returns400() throws Exception {
        String activityId = createActivity("Old", "2025-01-06T09:00:00");

        String json = objectMapper.writeValueAsString(Map.of(
                "title", "",
                "occurs_at", "2025-01-07T10:00:00"
        ));

        mockMvc.perform(put("/trips/{tripId}/activities/{activityId}", tripId, activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateActivity_InvalidOccursAt_Returns400() throws Exception {
        String activityId = createActivity("Old", "2025-01-06T09:00:00");

        String json = objectMapper.writeValueAsString(Map.of(
                "title", "New",
                "occurs_at", "bad-date"
        ));

        mockMvc.perform(put("/trips/{tripId}/activities/{activityId}", tripId, activityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateActivity_NonExistentActivity_Returns404() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of(
                "title", "New",
                "occurs_at", "2025-01-07T10:00:00"
        ));

        mockMvc.perform(put("/trips/{tripId}/activities/{activityId}", tripId, "00000000-0000-0000-0000-000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    // --- DELETE ---

    @Test
    void deleteActivity_ValidId_Returns204() throws Exception {
        String activityId = createActivity("To Delete", "2025-01-06T09:00:00");

        mockMvc.perform(delete("/trips/{tripId}/activities/{activityId}", tripId, activityId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/trips/{tripId}/activities/{activityId}", tripId, activityId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteActivity_NonExistentActivity_Returns404() throws Exception {
        mockMvc.perform(delete("/trips/{tripId}/activities/{activityId}", tripId, "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }
}
