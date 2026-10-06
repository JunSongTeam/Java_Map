package com.example.mapjava.track;

import java.time.Instant;

public record TrackPointResponse(
        double latitude,
        double longitude,
        Instant recordedAt,
        String address,
        Double accuracy,
        Double speed
) {
}
