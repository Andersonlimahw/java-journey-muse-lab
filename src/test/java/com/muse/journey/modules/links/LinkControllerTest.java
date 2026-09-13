package com.muse.journey.modules.links;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Covers the links slice ({@code /trips/{id}/links}), which had no tests in
 * the original project.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LinkControllerTest {

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
                "emails_to_invite", java.util.List.of()));

        var result = mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tripJson))
                .andExpect(status().isOk())
                .andReturn();

        tripId = objectMapper.readTree(result.getResponse().getContentAsString()).get("uuid").asText();
    }

    private String createLink(String title, String url) throws Exception {
        String json = objectMapper.writeValueAsString(Map.of("title", title, "url", url));
        var result = mockMvc.perform(post("/trips/{id}/links", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("linkId").asText();
    }

    @Test
    void createLink_ValidInput_Returns200() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of("title", "Airbnb", "url", "https://airbnb.com/rooms/1"));

        mockMvc.perform(post("/trips/{id}/links", tripId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.linkId").isString());
    }

    @Test
    void createLink_NonExistentTrip_Returns404() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of("title", "Airbnb", "url", "https://airbnb.com"));

        mockMvc.perform(post("/trips/{id}/links", "00000000-0000-0000-0000-000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLinks_ValidTrip_Returns200() throws Exception {
        createLink("Airbnb", "https://airbnb.com/rooms/1");

        mockMvc.perform(get("/trips/{id}/links", tripId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getLinks_NonExistentTrip_Returns404() throws Exception {
        mockMvc.perform(get("/trips/{id}/links", "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getLink_ValidId_Returns200() throws Exception {
        String linkId = createLink("Airbnb", "https://airbnb.com/rooms/1");

        mockMvc.perform(get("/trips/{tripId}/links/{linkId}", tripId, linkId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(linkId))
                .andExpect(jsonPath("$.title").value("Airbnb"));
    }

    @Test
    void getLink_LinkFromAnotherTrip_Returns404() throws Exception {
        String linkId = createLink("Airbnb", "https://airbnb.com/rooms/1");

        String otherTripJson = objectMapper.writeValueAsString(Map.of(
                "destination", "Rio",
                "starts_at", "2025-02-01T10:00:00",
                "ends_at", "2025-02-10T10:00:00",
                "owner_name", "Jane",
                "owner_email", "jane@email.com",
                "emails_to_invite", java.util.List.of()));
        var otherResult = mockMvc.perform(post("/trips")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(otherTripJson))
                .andExpect(status().isOk())
                .andReturn();
        String otherTripId = objectMapper.readTree(otherResult.getResponse().getContentAsString()).get("uuid").asText();

        mockMvc.perform(get("/trips/{tripId}/links/{linkId}", otherTripId, linkId))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateLink_ValidInput_Returns200() throws Exception {
        String linkId = createLink("Old", "https://old.example.com");

        String json = objectMapper.writeValueAsString(Map.of("title", "New", "url", "https://new.example.com"));

        mockMvc.perform(put("/trips/{tripId}/links/{linkId}", tripId, linkId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New"))
                .andExpect(jsonPath("$.url").value("https://new.example.com"));
    }

    @Test
    void updateLink_NonExistentLink_Returns404() throws Exception {
        String json = objectMapper.writeValueAsString(Map.of("title", "New", "url", "https://new.example.com"));

        mockMvc.perform(put("/trips/{tripId}/links/{linkId}", tripId, "00000000-0000-0000-0000-000000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteLink_ValidId_Returns204() throws Exception {
        String linkId = createLink("To Delete", "https://delete.example.com");

        mockMvc.perform(delete("/trips/{tripId}/links/{linkId}", tripId, linkId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/trips/{tripId}/links/{linkId}", tripId, linkId))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteLink_NonExistentLink_Returns404() throws Exception {
        mockMvc.perform(delete("/trips/{tripId}/links/{linkId}", tripId, "00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound());
    }
}
