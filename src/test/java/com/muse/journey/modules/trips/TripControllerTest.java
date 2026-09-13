package com.muse.journey.modules.trips;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TripControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TripRepository tripRepository;

    private Trip savedTrip;

    @BeforeEach
    void setUp() {
        tripRepository.deleteAll();

        Trip trip = new Trip();
        trip.setDestination("Lisbon");
        trip.setStartsAt(LocalDateTime.parse("2026-08-01T10:00:00"));
        trip.setEndsAt(LocalDateTime.parse("2026-08-10T10:00:00"));
        trip.setOwnerName("Lemon Traveler");
        trip.setOwnerEmail("traveler@example.com");
        trip.setConfirmed(true);
        savedTrip = tripRepository.save(trip);
    }

    @Test
    void overview_shouldReturnTripSummary() throws Exception {
        mockMvc.perform(get("/trips/{id}/overview", savedTrip.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(savedTrip.getId().toString()))
                .andExpect(jsonPath("$.destination").value("Lisbon"))
                .andExpect(jsonPath("$.confirmed").value(true))
                .andExpect(jsonPath("$.ownerName").value("Lemon Traveler"))
                .andExpect(jsonPath("$.ownerEmail").value("traveler@example.com"));
    }

    @Test
    void overview_shouldReturn404_whenTripDoesNotExist() throws Exception {
        mockMvc.perform(get("/trips/{id}/overview", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
