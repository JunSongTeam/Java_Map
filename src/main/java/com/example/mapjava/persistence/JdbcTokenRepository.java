package com.example.mapjava.persistence;

import java.util.Optional;

import com.example.mapjava.auth.AuthSession;
import com.example.mapjava.auth.TokenRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db")
public class JdbcTokenRepository extends JdbcRepositorySupport implements TokenRepository {

    public JdbcTokenRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public Optional<AuthSession> findByToken(String token) {
        return jdbc.query(
                "SELECT token, user_id, issued_at, expires_at FROM auth_sessions WHERE token = ?",
                (resultSet, rowNum) -> new AuthSession(
                        resultSet.getString("token"),
                        uuid(resultSet.getString("user_id")),
                        instant(resultSet.getTimestamp("issued_at")),
                        instant(resultSet.getTimestamp("expires_at"))
                ),
                token
        ).stream().findFirst();
    }

    @Override
    public AuthSession save(AuthSession session) {
        jdbc.update(
                "MERGE INTO auth_sessions (token, user_id, issued_at, expires_at) KEY (token) VALUES (?, ?, ?, ?)",
                session.token(),
                session.userId().toString(),
                timestamp(session.issuedAt()),
                timestamp(session.expiresAt())
        );
        return session;
    }

    @Override
    public void deleteByToken(String token) {
        jdbc.update("DELETE FROM auth_sessions WHERE token = ?", token);
    }
}
