package com.example.mapjava.friend;

import java.time.Instant;
import java.util.UUID;

public record FriendRequest(
        UUID id,
        UUID requesterUserId,
        UUID receiverUserId,
        FriendRequestStatus status,
        Instant createdAt,
        Instant respondedAt
) {
}
