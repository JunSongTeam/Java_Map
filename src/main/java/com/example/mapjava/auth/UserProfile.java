package com.example.mapjava.auth;

import java.time.Instant;
import java.util.UUID;

public record UserProfile(
        UUID userId,
        String phone,
        String nickname,
        String avatarUrl,
        String memberStatus,
        Instant memberExpiresAt
) {
}
