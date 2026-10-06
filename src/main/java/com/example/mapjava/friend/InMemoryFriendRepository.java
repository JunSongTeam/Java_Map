package com.example.mapjava.friend;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryFriendRepository implements FriendRepository {

    private final ConcurrentMap<String, FriendRelation> relationsByPair = new ConcurrentHashMap<>();

    @Override
    public Optional<FriendRelation> findBetween(UUID firstUserId, UUID secondUserId) {
        return Optional.ofNullable(relationsByPair.get(key(firstUserId, secondUserId)));
    }

    @Override
    public List<FriendRelation> findByUserId(UUID userId) {
        return relationsByPair.values()
                .stream()
                .filter(relation -> relation.firstUserId().equals(userId) || relation.secondUserId().equals(userId))
                .sorted(Comparator.comparing(FriendRelation::createdAt).reversed())
                .toList();
    }

    @Override
    public FriendRelation save(FriendRelation relation) {
        relationsByPair.put(key(relation.firstUserId(), relation.secondUserId()), relation);
        return relation;
    }

    @Override
    public void deleteBetween(UUID firstUserId, UUID secondUserId) {
        relationsByPair.remove(key(firstUserId, secondUserId));
    }

    private static String key(UUID firstUserId, UUID secondUserId) {
        if (firstUserId.compareTo(secondUserId) <= 0) {
            return firstUserId + ":" + secondUserId;
        }

        return secondUserId + ":" + firstUserId;
    }
}
