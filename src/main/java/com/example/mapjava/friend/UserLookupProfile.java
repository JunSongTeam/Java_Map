package com.example.mapjava.friend;

import java.util.UUID;

public record UserLookupProfile(
        UUID userId,
        String phone,
        String nickname,
        String avatarUrl
) {
}
