package com.example.mapjava.friend;

import java.util.Optional;
import java.util.UUID;

public interface FriendRemarkRepository {

    Optional<String> findRemark(UUID userId, UUID friendUserId);

    void saveRemark(UUID userId, UUID friendUserId, String remark);

    void deleteRemarksBetween(UUID firstUserId, UUID secondUserId);
}
