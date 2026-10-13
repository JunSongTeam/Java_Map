package com.example.mapjava.membership;

import java.time.Instant;
import java.util.UUID;

public record MembershipOrder(
        UUID orderId,
        UUID userId,
        String productId,
        String paymentProvider,
        MembershipOrderStatus status,
        long priceCents,
        String currency,
        Instant createdAt,
        Instant expiresAt,
        String transactionId,
        Instant paidAt
) {
}
