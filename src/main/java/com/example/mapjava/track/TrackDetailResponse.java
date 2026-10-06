package com.example.mapjava.track;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TrackDetailResponse(
        UUID trackId,
        LocalDate date,
        String startAddress,
        String endAddress,
        Instant startedAt,
        Instant endedAt,
        double distanceMeters,
        int pointCount,
        List<TrackPointResponse> points
) {
}
