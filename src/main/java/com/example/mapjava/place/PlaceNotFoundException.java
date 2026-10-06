package com.example.mapjava.place;

import java.util.UUID;

public class PlaceNotFoundException extends RuntimeException {

    public PlaceNotFoundException(UUID id) {
        super("Place not found: " + id);
    }
}
