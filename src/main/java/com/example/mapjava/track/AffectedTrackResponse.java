package com.example.mapjava.track;

import java.time.LocalDate;
import java.util.UUID;

public record AffectedTrackResponse(
        UUID trackId,
        LocalDate date
) {
}
