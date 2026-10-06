package com.example.mapjava.place;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPlaceRepository implements PlaceRepository {

    private final ConcurrentMap<UUID, Place> places = new ConcurrentHashMap<>();

    @Override
    public List<Place> findAll() {
        return places.values()
                .stream()
                .sorted(Comparator.comparing(Place::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<Place> findById(UUID id) {
        return Optional.ofNullable(places.get(id));
    }

    @Override
    public Place save(Place place) {
        places.put(place.id(), place);
        return place;
    }

    @Override
    public void deleteById(UUID id) {
        places.remove(id);
    }

    @Override
    public boolean existsById(UUID id) {
        return places.containsKey(id);
    }
}
