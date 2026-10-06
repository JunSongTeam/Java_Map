package com.example.mapjava.track;

import java.time.Instant;

public record TrackPoint(
        double latitude,
        double longitude,
        Instant recordedAt,
        String address,
        Double accuracy,
        Double speed
) {
}
