package com.muse.journey.modules.participants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.muse.journey.modules.trips.Trip;
import com.muse.journey.modules.trips.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ParticipantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private TripRepository tripRepository;

    private Trip savedTrip;

    @BeforeEach
    void setUp() {
        participantRepository.deleteAll();
        tripRepository.deleteAll();

        Trip trip = new Trip();
        trip.setDestination("Test City");
        trip.setStartsAt(LocalDateTime.parse("2025-01-01T10:00:00"));
        trip.setEndsAt(LocalDateTime.parse("2025-01-10T10:00:00"));
        trip.setOwnerName("Owner");
        trip.setOwnerEmail("owner@email.com");
        trip.setConfirmed(false);
        savedTrip = tripRepository.save(trip);
    }

    @Test
    void create_shouldReturnParticipantId() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", "test@email.com");
        payload.put("name", "Test User");
        payload.put("trip_id", savedTrip.getId().toString());

        mockMvc.perform(post("/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString());
    }

    @Test
    void create_shouldReturn400_whenEmailMissing() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("trip_id", savedTrip.getId().toString());

        mockMvc.perform(post("/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("email is required"));
    }

    @Test
    void create_shouldReturn400_whenEmailBlank() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", "");
        payload.put("trip_id", savedTrip.getId().toString());

        mockMvc.perform(post("/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("email is required"));
    }

    @Test
    void create_shouldReturn400_whenTripIdMissing() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", "test@email.com");

        mockMvc.perform(post("/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("trip_id is required"));
    }

    @Test
    void create_shouldReturn400_whenTripNotFound() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", "test@email.com");
        payload.put("trip_id", UUID.randomUUID().toString());

        mockMvc.perform(post("/participants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("trip not found"));
    }

    @Test
    void getById_shouldReturnParticipant() throws Exception {
        Participant participant = participantRepository.save(new Participant("test@email.com", savedTrip));

        mockMvc.perform(get("/participants/{id}", participant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(participant.getId().toString()))
                .andExpect(jsonPath("$.email").value("test@email.com"))
                .andExpect(jsonPath("$.isConfirmed").value(false));
    }

    @Test
    void getById_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(get("/participants/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_shouldReturnAllParticipants() throws Exception {
        participantRepository.save(new Participant("a@email.com", savedTrip));
        participantRepository.save(new Participant("b@email.com", savedTrip));

        mockMvc.perform(get("/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void update_shouldReturnUpdatedParticipant() throws Exception {
        Participant participant = participantRepository.save(new Participant("old@email.com", savedTrip));

        Map<String, Object> payload = new HashMap<>();
        payload.put("email", "new@email.com");
        payload.put("name", "Updated Name");

        mockMvc.perform(put("/participants/{id}", participant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@email.com"))
                .andExpect(jsonPath("$.name").value("Updated Name"));
    }

    @Test
    void update_shouldReturn400_whenEmailMissing() throws Exception {
        Participant participant = participantRepository.save(new Participant("test@email.com", savedTrip));

        Map<String, Object> payload = new HashMap<>();
        payload.put("name", "Name");

        mockMvc.perform(put("/participants/{id}", participant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("email is required"));
    }

    @Test
    void update_shouldReturn404_whenNotFound() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("email", "test@email.com");

        mockMvc.perform(put("/participants/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        Participant participant = participantRepository.save(new Participant("test@email.com", savedTrip));

        mockMvc.perform(delete("/participants/{id}", participant.getId()))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404_whenNotFound() throws Exception {
        mockMvc.perform(delete("/participants/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
