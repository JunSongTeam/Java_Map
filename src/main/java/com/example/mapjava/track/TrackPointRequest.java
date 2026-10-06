package com.example.mapjava.track;

import java.time.Instant;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrackPointRequest(
        @DecimalMin(value = "-90.0", message = "latitude must be greater than or equal to -90")
        @DecimalMax(value = "90.0", message = "latitude must be less than or equal to 90")
        double latitude,

        @DecimalMin(value = "-180.0", message = "longitude must be greater than or equal to -180")
        @DecimalMax(value = "180.0", message = "longitude must be less than or equal to 180")
        double longitude,

        @NotNull(message = "recordedAt is required")
        Instant recordedAt,

        @Size(max = 200, message = "address must be at most 200 characters")
        String address,

        @DecimalMin(value = "0.0", message = "accuracy must be greater than or equal to 0")
        Double accuracy,

        @DecimalMin(value = "0.0", message = "speed must be greater than or equal to 0")
        Double speed
) {
}
