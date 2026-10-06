package com.example.mapjava.place;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlaceRepository {

    List<Place> findAll();

    Optional<Place> findById(UUID id);

    Place save(Place place);

    void deleteById(UUID id);

    boolean existsById(UUID id);
}
