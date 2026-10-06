package com.example.mapjava.friend;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FriendRequestRepository {

    Optional<FriendRequest> findById(UUID id);

    Optional<FriendRequest> findPendingByRequesterAndReceiver(UUID requesterUserId, UUID receiverUserId);

    Optional<FriendRequest> findPendingBetween(UUID firstUserId, UUID secondUserId);

    List<FriendRequest> findPendingByReceiverId(UUID receiverUserId);

    FriendRequest save(FriendRequest request);
}
