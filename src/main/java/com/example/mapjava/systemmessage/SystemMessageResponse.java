package com.example.mapjava.systemmessage;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record SystemMessageResponse(
        UUID messageId,
        String type,
        String title,
        String content,
        boolean read,
        Map<String, String> payload,
        Instant createdAt,
        Instant readAt
) {
}
