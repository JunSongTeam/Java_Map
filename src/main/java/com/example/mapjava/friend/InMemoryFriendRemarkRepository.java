package com.example.mapjava.friend;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryFriendRemarkRepository implements FriendRemarkRepository {

    private final ConcurrentMap<String, String> remarksByUserAndFriend = new ConcurrentHashMap<>();

    @Override
    public Optional<String> findRemark(UUID userId, UUID friendUserId) {
        return Optional.ofNullable(remarksByUserAndFriend.get(key(userId, friendUserId)));
    }

    @Override
    public void saveRemark(UUID userId, UUID friendUserId, String remark) {
        String key = key(userId, friendUserId);
        if (remark == null || remark.isBlank()) {
            remarksByUserAndFriend.remove(key);
            return;
        }

        remarksByUserAndFriend.put(key, remark);
    }

    @Override
    public void deleteRemarksBetween(UUID firstUserId, UUID secondUserId) {
        remarksByUserAndFriend.remove(key(firstUserId, secondUserId));
        remarksByUserAndFriend.remove(key(secondUserId, firstUserId));
    }

    private static String key(UUID userId, UUID friendUserId) {
        return userId + ":" + friendUserId;
    }
}
