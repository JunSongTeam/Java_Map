package com.example.mapjava.place;

import java.time.Instant;
import java.util.UUID;

public record Place(
        UUID id,
        String name,
        String category,
        double latitude,
        double longitude,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
