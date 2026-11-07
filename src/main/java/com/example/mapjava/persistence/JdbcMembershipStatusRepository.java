package com.example.mapjava.persistence;

import java.util.Optional;
import java.util.UUID;

import com.example.mapjava.membership.MembershipStatus;
import com.example.mapjava.membership.MembershipStatusRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db")
public class JdbcMembershipStatusRepository extends JdbcRepositorySupport implements MembershipStatusRepository {

    public JdbcMembershipStatusRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public Optional<MembershipStatus> findByUserId(UUID userId) {
        return jdbc.query(
                "SELECT user_id, expires_at, updated_at FROM membership_status WHERE user_id = ?",
                (resultSet, rowNum) -> new MembershipStatus(
                        uuid(resultSet.getString("user_id")),
                        instant(resultSet.getTimestamp("expires_at")),
                        instant(resultSet.getTimestamp("updated_at"))
                ),
                userId.toString()
        ).stream().findFirst();
    }

    @Override
    public MembershipStatus save(MembershipStatus status) {
        jdbc.update(
                "MERGE INTO membership_status (user_id, expires_at, updated_at) KEY (user_id) VALUES (?, ?, ?)",
                status.userId().toString(),
                timestamp(status.expiresAt()),
                timestamp(status.updatedAt())
        );
        return status;
    }
}
