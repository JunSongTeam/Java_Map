package com.example.mapjava.membership;

public record MembershipProduct(
        String productId,
        String title,
        String description,
        int durationDays,
        long priceCents,
        String currency,
        String appleProductId,
        String time,
        String price,
        String originalPrice,
        String unitPrice,
        String orderID,
        String status
) {
}
