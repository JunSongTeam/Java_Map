package com.example.mapjava.auth;

import java.time.Clock;
import java.time.Instant;

public record VerificationCode(
        String channel,
        String target,
        String codeHash,
        Instant createdAt,
        Instant expiresAt
) {

    public boolean isExpired(Clock clock) {
        return !Instant.now(clock).isBefore(expiresAt);
    }
}
