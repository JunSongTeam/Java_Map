package com.example.mapjava.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.mapjava.friend.FriendRelation;
import com.example.mapjava.friend.FriendRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db")
public class JdbcFriendRepository extends JdbcRepositorySupport implements FriendRepository {

    public JdbcFriendRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public Optional<FriendRelation> findBetween(UUID firstUserId, UUID secondUserId) {
        UUID first = firstUserId.compareTo(secondUserId) <= 0 ? firstUserId : secondUserId;
        UUID second = first.equals(firstUserId) ? secondUserId : firstUserId;
        return jdbc.query(
                "SELECT id, first_user_id, second_user_id, created_at FROM friend_relations "
                        + "WHERE first_user_id = ? AND second_user_id = ?",
                this::map,
                first.toString(),
                second.toString()
        ).stream().findFirst();
    }

    @Override
    public List<FriendRelation> findByUserId(UUID userId) {
        return jdbc.query(
                "SELECT id, first_user_id, second_user_id, created_at FROM friend_relations "
                        + "WHERE first_user_id = ? OR second_user_id = ? ORDER BY created_at DESC",
                this::map,
                userId.toString(),
                userId.toString()
        );
    }

    @Override
    public FriendRelation save(FriendRelation relation) {
        UUID first = relation.firstUserId().compareTo(relation.secondUserId()) <= 0
                ? relation.firstUserId() : relation.secondUserId();
        UUID second = first.equals(relation.firstUserId())
                ? relation.secondUserId() : relation.firstUserId();
        jdbc.update(
                "MERGE INTO friend_relations (id, first_user_id, second_user_id, created_at) "
                        + "KEY (first_user_id, second_user_id) VALUES (?, ?, ?, ?)",
                relation.id().toString(),
                first.toString(),
                second.toString(),
                timestamp(relation.createdAt())
        );
        return new FriendRelation(relation.id(), first, second, relation.createdAt());
    }

    @Override
    public void deleteBetween(UUID firstUserId, UUID secondUserId) {
        UUID first = firstUserId.compareTo(secondUserId) <= 0 ? firstUserId : secondUserId;
        UUID second = first.equals(firstUserId) ? secondUserId : firstUserId;
        jdbc.update(
                "DELETE FROM friend_relations WHERE first_user_id = ? AND second_user_id = ?",
                first.toString(),
                second.toString()
        );
    }

    private FriendRelation map(java.sql.ResultSet resultSet, int rowNum) throws java.sql.SQLException {
        return new FriendRelation(
                uuid(resultSet.getString("id")),
                uuid(resultSet.getString("first_user_id")),
                uuid(resultSet.getString("second_user_id")),
                instant(resultSet.getTimestamp("created_at"))
        );
    }
}
