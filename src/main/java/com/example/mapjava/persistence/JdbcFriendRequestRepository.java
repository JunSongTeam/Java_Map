package com.example.mapjava.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.mapjava.friend.FriendRequest;
import com.example.mapjava.friend.FriendRequestRepository;
import com.example.mapjava.friend.FriendRequestStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db")
public class JdbcFriendRequestRepository extends JdbcRepositorySupport implements FriendRequestRepository {

    public JdbcFriendRequestRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public Optional<FriendRequest> findById(UUID id) {
        return jdbc.query(
                "SELECT id, requester_user_id, receiver_user_id, status, created_at, responded_at "
                        + "FROM friend_requests WHERE id = ?",
                this::map,
                id.toString()
        ).stream().findFirst();
    }

    @Override
    public Optional<FriendRequest> findPendingByRequesterAndReceiver(UUID requesterUserId, UUID receiverUserId) {
        return jdbc.query(
                "SELECT id, requester_user_id, receiver_user_id, status, created_at, responded_at "
                        + "FROM friend_requests WHERE requester_user_id = ? AND receiver_user_id = ? "
                        + "AND status = 'PENDING' ORDER BY created_at DESC LIMIT 1",
                this::map,
                requesterUserId.toString(),
                receiverUserId.toString()
        ).stream().findFirst();
    }

    @Override
    public Optional<FriendRequest> findPendingBetween(UUID firstUserId, UUID secondUserId) {
        return jdbc.query(
                "SELECT id, requester_user_id, receiver_user_id, status, created_at, responded_at "
                        + "FROM friend_requests WHERE status = 'PENDING' AND "
                        + "((requester_user_id = ? AND receiver_user_id = ?) OR "
                        + "(requester_user_id = ? AND receiver_user_id = ?)) "
                        + "ORDER BY created_at DESC LIMIT 1",
                this::map,
                firstUserId.toString(),
                secondUserId.toString(),
                secondUserId.toString(),
                firstUserId.toString()
        ).stream().findFirst();
    }

    @Override
    public List<FriendRequest> findPendingByReceiverId(UUID receiverUserId) {
        return jdbc.query(
                "SELECT id, requester_user_id, receiver_user_id, status, created_at, responded_at "
                        + "FROM friend_requests WHERE receiver_user_id = ? AND status = 'PENDING' "
                        + "ORDER BY created_at DESC",
                this::map,
                receiverUserId.toString()
        );
    }

    @Override
    public FriendRequest save(FriendRequest request) {
        jdbc.update(
                "MERGE INTO friend_requests (id, requester_user_id, receiver_user_id, status, created_at, responded_at) "
                        + "KEY (id) VALUES (?, ?, ?, ?, ?, ?)",
                request.id().toString(),
                request.requesterUserId().toString(),
                request.receiverUserId().toString(),
                request.status().name(),
                timestamp(request.createdAt()),
                timestamp(request.respondedAt())
        );
        return request;
    }

    private FriendRequest map(java.sql.ResultSet resultSet, int rowNum) throws java.sql.SQLException {
        return new FriendRequest(
                uuid(resultSet.getString("id")),
                uuid(resultSet.getString("requester_user_id")),
                uuid(resultSet.getString("receiver_user_id")),
                FriendRequestStatus.valueOf(resultSet.getString("status")),
                instant(resultSet.getTimestamp("created_at")),
                instant(resultSet.getTimestamp("responded_at"))
        );
    }
}
