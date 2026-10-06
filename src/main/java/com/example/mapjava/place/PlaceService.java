package com.example.mapjava.place;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final Clock clock;

    public PlaceService(PlaceRepository placeRepository, Clock clock) {
        this.placeRepository = placeRepository;
        this.clock = clock;
    }

    public List<Place> findPlaces(String category, String query) {
        return placeRepository.findAll()
                .stream()
                .filter(place -> matchesCategory(place, category))
                .filter(place -> matchesQuery(place, query))
                .toList();
    }

    public Place getPlace(UUID id) {
        return placeRepository.findById(id)
                .orElseThrow(() -> new PlaceNotFoundException(id));
    }

    public Place createPlace(PlaceRequest request) {
        Instant now = Instant.now(clock);
        Place place = new Place(
                UUID.randomUUID(),
                request.name().trim(),
                request.category().trim(),
                request.latitude(),
                request.longitude(),
                normalizeDescription(request.description()),
                now,
                now
        );

        return placeRepository.save(place);
    }

    public Place updatePlace(UUID id, PlaceRequest request) {
        Place existing = getPlace(id);
        Place updated = new Place(
                existing.id(),
                request.name().trim(),
                request.category().trim(),
                request.latitude(),
                request.longitude(),
                normalizeDescription(request.description()),
                existing.createdAt(),
                Instant.now(clock)
        );

        return placeRepository.save(updated);
    }

    public void deletePlace(UUID id) {
        if (!placeRepository.existsById(id)) {
            throw new PlaceNotFoundException(id);
        }

        placeRepository.deleteById(id);
    }

    private static boolean matchesCategory(Place place, String category) {
        if (category == null || category.isBlank()) {
            return true;
        }

        return place.category().equalsIgnoreCase(category.trim());
    }

    private static boolean matchesQuery(Place place, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        return place.name().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || place.category().toLowerCase(Locale.ROOT).contains(normalizedQuery)
                || place.description().toLowerCase(Locale.ROOT).contains(normalizedQuery);
    }

    private static String normalizeDescription(String description) {
        return description == null ? "" : description.trim();
    }
}
