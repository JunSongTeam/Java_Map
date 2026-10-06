package com.example.mapjava.friend;

import java.time.Instant;
import java.util.UUID;

public record FriendResponse(
        UUID friendUserId,
        String phone,
        String nickname,
        String remark,
        String avatarUrl,
        boolean alreadyFriend,
        Instant createdAt
) {
}
