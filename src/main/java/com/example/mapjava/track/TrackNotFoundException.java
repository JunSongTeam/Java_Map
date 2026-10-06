package com.example.mapjava.track;

import java.util.UUID;

public class TrackNotFoundException extends RuntimeException {

    public TrackNotFoundException(UUID id) {
        super("Track not found: " + id);
    }
}
