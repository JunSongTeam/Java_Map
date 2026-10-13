package com.example.mapjava.membership;

import java.time.Instant;

public record MembershipStatusResponse(
        String memberStatus,
        Instant memberExpiresAt,
        boolean active
) {
}
