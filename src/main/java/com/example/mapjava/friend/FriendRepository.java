package com.example.mapjava.friend;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendRepository {

    Optional<FriendRelation> findBetween(UUID firstUserId, UUID secondUserId);

    List<FriendRelation> findByUserId(UUID userId);

    FriendRelation save(FriendRelation relation);

    void deleteBetween(UUID firstUserId, UUID secondUserId);
}
