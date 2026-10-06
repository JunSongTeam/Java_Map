package com.example.mapjava.auth;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TokenSettings {

    private final boolean neverExpires;
    private final long expirationSeconds;

    public TokenSettings(
            @Value("${auth.token.never-expires:true}") boolean neverExpires,
            @Value("${auth.token.expiration-seconds:604800}") long expirationSeconds
    ) {
        if (!neverExpires && expirationSeconds <= 0) {
            throw new IllegalArgumentException("auth.token.expiration-seconds must be positive");
        }

        this.neverExpires = neverExpires;
        this.expirationSeconds = expirationSeconds;
    }

    public Instant expiresAt(Clock clock) {
        if (neverExpires) {
            return null;
        }

        return Instant.now(clock).plusSeconds(expirationSeconds);
    }

    public Long expiresInSeconds() {
        return neverExpires ? null : expirationSeconds;
    }
}
