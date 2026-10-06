package com.example.mapjava.auth;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

public record AuthSession(
        String token,
        UUID userId,
        Instant issuedAt,
        Instant expiresAt
) {

    public boolean isExpired(Clock clock) {
        return expiresAt != null && !Instant.now(clock).isBefore(expiresAt);
    }
}
