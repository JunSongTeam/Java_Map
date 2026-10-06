package com.example.mapjava.auth;

import java.time.Instant;

public record VerificationCodeResponse(
        String phone,
        long expiresInSeconds,
        Instant expiresAt,
        String devCode
) {
}
