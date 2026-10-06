package com.example.mapjava.auth;

import java.time.Instant;
import java.util.UUID;

public record AppUser(
        UUID id,
        String phone,
        String displayName,
        String avatarUrl,
        Instant createdAt
) {
}
