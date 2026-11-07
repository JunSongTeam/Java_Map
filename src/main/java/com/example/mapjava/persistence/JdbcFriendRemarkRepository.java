package com.example.mapjava.persistence;

import java.util.Optional;
import java.util.UUID;

import com.example.mapjava.friend.FriendRemarkRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db")
public class JdbcFriendRemarkRepository extends JdbcRepositorySupport implements FriendRemarkRepository {

    public JdbcFriendRemarkRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public Optional<String> findRemark(UUID userId, UUID friendUserId) {
        return jdbc.query(
                "SELECT remark FROM friend_remarks WHERE user_id = ? AND friend_user_id = ?",
                (resultSet, rowNum) -> resultSet.getString("remark"),
                userId.toString(),
                friendUserId.toString()
        ).stream().findFirst();
    }

    @Override
    public void saveRemark(UUID userId, UUID friendUserId, String remark) {
        if (remark == null || remark.isBlank()) {
            jdbc.update(
                    "DELETE FROM friend_remarks WHERE user_id = ? AND friend_user_id = ?",
                    userId.toString(),
                    friendUserId.toString()
            );
            return;
        }

        jdbc.update(
                "MERGE INTO friend_remarks (user_id, friend_user_id, remark) "
                        + "KEY (user_id, friend_user_id) VALUES (?, ?, ?)",
                userId.toString(),
                friendUserId.toString(),
                remark
        );
    }

    @Override
    public void deleteRemarksBetween(UUID firstUserId, UUID secondUserId) {
        jdbc.update(
                "DELETE FROM friend_remarks WHERE (user_id = ? AND friend_user_id = ?) "
                        + "OR (user_id = ? AND friend_user_id = ?)",
                firstUserId.toString(),
                secondUserId.toString(),
                secondUserId.toString(),
                firstUserId.toString()
        );
    }
}
