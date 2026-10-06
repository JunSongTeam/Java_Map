package com.example.mapjava.track;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record Track(
        UUID id,
        UUID userId,
        LocalDate date,
        List<TrackPoint> points,
        Instant createdAt,
        Instant updatedAt
) {
}
