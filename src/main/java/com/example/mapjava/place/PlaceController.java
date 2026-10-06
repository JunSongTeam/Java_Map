package com.example.mapjava.place;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping
    public List<Place> findPlaces(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, name = "q") String query
    ) {
        return placeService.findPlaces(category, query);
    }

    @GetMapping("/{id}")
    public Place getPlace(@PathVariable UUID id) {
        return placeService.getPlace(id);
    }

    @PostMapping
    public ResponseEntity<Place> createPlace(@Valid @RequestBody PlaceRequest request) {
        Place place = placeService.createPlace(request);
        return ResponseEntity
                .created(URI.create("/api/places/" + place.id()))
                .body(place);
    }

    @PutMapping("/{id}")
    public Place updatePlace(@PathVariable UUID id, @Valid @RequestBody PlaceRequest request) {
        return placeService.updatePlace(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlace(@PathVariable UUID id) {
        placeService.deletePlace(id);
        return ResponseEntity.noContent().build();
    }
}
