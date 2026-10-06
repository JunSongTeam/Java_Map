package com.example.mapjava.friend;

import java.time.Instant;
import java.util.UUID;

public record FriendRequestResponse(
        UUID requestId,
        String status,
        String message,
        UserLookupProfile requester,
        UserLookupProfile receiver,
        Instant createdAt
) {
}
