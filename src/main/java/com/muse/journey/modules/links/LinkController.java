package com.muse.journey.modules.links;

import com.muse.journey.modules.trips.Trip;
import com.muse.journey.modules.trips.TripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Vertical slice "links": every endpoint is trip-scoped, so each handler
 * first loads the trip and then delegates to {@link LinkService}.
 */
@RestController
@RequestMapping("/trips/{id}/links")
public class LinkController {

    @Autowired
    private TripRepository tripRepository;

    @Autowired
    private LinkService linkService;

    @PostMapping
    public ResponseEntity<LinkResponse> registerLink(@PathVariable UUID id, @RequestBody LinkRequestPayload payload) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        LinkResponse linkResponse = this.linkService.registerLink(payload, trip.get());
        return ResponseEntity.ok(linkResponse);
    }

    @GetMapping
    public ResponseEntity<List<LinkData>> getAllLinks(@PathVariable UUID id) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        List<LinkData> linkDataList = this.linkService.getAllLinksFromTrip(id);
        return ResponseEntity.ok(linkDataList);
    }

    @GetMapping("/{linkId}")
    public ResponseEntity<LinkData> getLink(@PathVariable UUID id, @PathVariable UUID linkId) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return this.linkService.getLink(linkId, trip.get())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{linkId}")
    public ResponseEntity<LinkData> updateLink(@PathVariable UUID id, @PathVariable UUID linkId, @RequestBody LinkRequestPayload payload) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return this.linkService.updateLink(linkId, payload, trip.get())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{linkId}")
    public ResponseEntity<Void> deleteLink(@PathVariable UUID id, @PathVariable UUID linkId) {
        Optional<Trip> trip = this.tripRepository.findById(id);
        if (trip.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        if (this.linkService.deleteLink(linkId, trip.get())) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
