package com.example.mapjava.track;

import java.time.LocalDate;
import java.util.UUID;

public record TrackSummaryResponse(
        UUID trackId,
        LocalDate date,
        String startAddress,
        String endAddress
) {
}
