package com.example.mapjava.persistence;

import java.util.Optional;
import java.util.UUID;

import com.example.mapjava.auth.AppUser;
import com.example.mapjava.auth.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db")
public class JdbcUserRepository extends JdbcRepositorySupport implements UserRepository {

    public JdbcUserRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public Optional<AppUser> findById(UUID id) {
        return jdbc.query(
                "SELECT id, phone, display_name, avatar_url, created_at FROM app_users WHERE id = ?",
                (resultSet, rowNum) -> new AppUser(
                        uuid(resultSet.getString("id")),
                        resultSet.getString("phone"),
                        resultSet.getString("display_name"),
                        resultSet.getString("avatar_url"),
                        instant(resultSet.getTimestamp("created_at"))
                ),
                id.toString()
        ).stream().findFirst();
    }

    @Override
    public Optional<AppUser> findByPhone(String phone) {
        return jdbc.query(
                "SELECT id, phone, display_name, avatar_url, created_at FROM app_users WHERE phone = ?",
                (resultSet, rowNum) -> new AppUser(
                        uuid(resultSet.getString("id")),
                        resultSet.getString("phone"),
                        resultSet.getString("display_name"),
                        resultSet.getString("avatar_url"),
                        instant(resultSet.getTimestamp("created_at"))
                ),
                phone
        ).stream().findFirst();
    }

    @Override
    public AppUser save(AppUser user) {
        jdbc.update(
                "MERGE INTO app_users (id, phone, display_name, avatar_url, created_at) KEY (id) VALUES (?, ?, ?, ?, ?)",
                user.id().toString(),
                user.phone(),
                user.displayName(),
                user.avatarUrl(),
                timestamp(user.createdAt())
        );
        return user;
    }
}
