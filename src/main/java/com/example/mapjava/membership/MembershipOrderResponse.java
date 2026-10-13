package com.example.mapjava.membership;

import java.time.Instant;
import java.util.UUID;

public record MembershipOrderResponse(
        UUID orderId,
        String productId,
        String productTitle,
        String paymentProvider,
        String status,
        long priceCents,
        String currency,
        Instant createdAt,
        Instant expiresAt,
        String transactionId,
        Instant paidAt,
        String memberStatus,
        Instant memberExpiresAt
) {
}
