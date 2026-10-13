package com.example.mapjava.membership;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMembershipOrderRequest(
        @NotBlank(message = "productId is required")
        String productId,

        @Size(max = 40, message = "paymentProvider must be at most 40 characters")
        String paymentProvider
) {
}
