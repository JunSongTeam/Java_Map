package com.example.mapjava.membership;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerifyMembershipOrderRequest(
        @NotNull(message = "orderId is required")
        UUID orderId,

        @Size(max = 120, message = "transactionId must be at most 120 characters")
        String transactionId,

        String receiptData
) {
}
