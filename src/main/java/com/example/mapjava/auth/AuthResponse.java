package com.example.mapjava.auth;

import java.time.Instant;

public record AuthResponse(
        String tokenType,
        String token,
        Instant expiresAt,
        Long expiresInSeconds
) {
}
