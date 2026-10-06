package com.example.mapjava.friend;

import java.time.Instant;
import java.util.UUID;

public record FriendRelation(
        UUID id,
        UUID firstUserId,
        UUID secondUserId,
        Instant createdAt
) {
}
