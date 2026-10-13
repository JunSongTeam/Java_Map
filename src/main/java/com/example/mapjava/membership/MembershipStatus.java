package com.example.mapjava.membership;

import java.time.Instant;
import java.util.UUID;

public record MembershipStatus(
        UUID userId,
        Instant expiresAt,
        Instant updatedAt
) {
}
