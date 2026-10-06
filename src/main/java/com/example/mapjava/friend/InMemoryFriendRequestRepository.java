package com.example.mapjava.friend;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryFriendRequestRepository implements FriendRequestRepository {

    private final ConcurrentMap<UUID, FriendRequest> requestsById = new ConcurrentHashMap<>();

    @Override
    public Optional<FriendRequest> findById(UUID id) {
        return Optional.ofNullable(requestsById.get(id));
    }

    @Override
    public Optional<FriendRequest> findPendingByRequesterAndReceiver(UUID requesterUserId, UUID receiverUserId) {
        return requestsById.values()
                .stream()
                .filter(request -> request.status() == FriendRequestStatus.PENDING)
                .filter(request -> request.requesterUserId().equals(requesterUserId))
                .filter(request -> request.receiverUserId().equals(receiverUserId))
                .max(Comparator.comparing(FriendRequest::createdAt));
    }

    @Override
    public Optional<FriendRequest> findPendingBetween(UUID firstUserId, UUID secondUserId) {
        return requestsById.values()
                .stream()
                .filter(request -> request.status() == FriendRequestStatus.PENDING)
                .filter(request -> isBetween(request, firstUserId, secondUserId))
                .max(Comparator.comparing(FriendRequest::createdAt));
    }

    @Override
    public List<FriendRequest> findPendingByReceiverId(UUID receiverUserId) {
        return requestsById.values()
                .stream()
                .filter(request -> request.status() == FriendRequestStatus.PENDING)
                .filter(request -> request.receiverUserId().equals(receiverUserId))
                .sorted(Comparator.comparing(FriendRequest::createdAt).reversed())
                .toList();
    }

    @Override
    public FriendRequest save(FriendRequest request) {
        requestsById.put(request.id(), request);
        return request;
    }

    private static boolean isBetween(FriendRequest request, UUID firstUserId, UUID secondUserId) {
        return request.requesterUserId().equals(firstUserId) && request.receiverUserId().equals(secondUserId)
                || request.requesterUserId().equals(secondUserId) && request.receiverUserId().equals(firstUserId);
    }
}
