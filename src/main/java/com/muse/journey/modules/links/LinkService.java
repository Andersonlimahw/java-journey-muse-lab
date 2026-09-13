package com.muse.journey.modules.links;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.muse.journey.modules.trips.Trip;

@Service
public class LinkService {

    @Autowired
    private LinkRepository repository;

    public LinkResponse registerLink(LinkRequestPayload payload, Trip trip) {
        Link newLink = new Link(payload.title(), payload.url(), trip);

        this.repository.save(newLink);

        return new LinkResponse(newLink.getId());
    }

    public List<LinkData> getAllLinksFromTrip(UUID tripId) {
        return this.repository.findByTripId(tripId).stream()
                .map(link -> new LinkData(link.getId(), link.getTitle(), link.getUrl()))
                .toList();
    }

    public Optional<LinkData> getLink(UUID linkId, Trip trip) {
        return repository.findByIdAndTrip(linkId, trip)
                .map(link -> new LinkData(link.getId(), link.getTitle(), link.getUrl()));
    }

    public Optional<LinkData> updateLink(UUID linkId, LinkRequestPayload payload, Trip trip) {
        return repository.findByIdAndTrip(linkId, trip)
                .map(link -> {
                    link.setTitle(payload.title());
                    link.setUrl(payload.url());
                    repository.save(link);
                    return new LinkData(link.getId(), link.getTitle(), link.getUrl());
                });
    }

    public boolean deleteLink(UUID linkId, Trip trip) {
        Optional<Link> link = repository.findByIdAndTrip(linkId, trip);
        if (link.isPresent()) {
            repository.delete(link.get());
            return true;
        }
        return false;
    }
}
