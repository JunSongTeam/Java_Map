package com.example.mapjava.persistence;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.example.mapjava.systemmessage.SystemMessage;
import com.example.mapjava.systemmessage.SystemMessageRepository;
import com.example.mapjava.systemmessage.SystemMessageType;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db")
public class JdbcSystemMessageRepository extends JdbcRepositorySupport implements SystemMessageRepository {

    private static final TypeReference<Map<String, String>> PAYLOAD_TYPE = new TypeReference<>() {};

    private final JdbcJsonCodec jsonCodec;

    public JdbcSystemMessageRepository(JdbcTemplate jdbc, JdbcJsonCodec jsonCodec) {
        super(jdbc);
        this.jsonCodec = jsonCodec;
    }

    @Override
    public Optional<SystemMessage> findById(UUID id) {
        return jdbc.query(
                "SELECT id, user_id, message_type, title, content, is_read, payload_json, created_at, read_at "
                        + "FROM system_messages WHERE id = ?",
                this::map,
                id.toString()
        ).stream().findFirst();
    }

    @Override
    public List<SystemMessage> findByUserId(UUID userId) {
        return jdbc.query(
                "SELECT id, user_id, message_type, title, content, is_read, payload_json, created_at, read_at "
                        + "FROM system_messages WHERE user_id = ? ORDER BY created_at DESC",
                this::map,
                userId.toString()
        );
    }

    @Override
    public boolean existsByUserIdAndPayloadValue(UUID userId, String payloadKey, String payloadValue) {
        return findByUserId(userId).stream()
                .anyMatch(message -> payloadValue.equals(message.payload().get(payloadKey)));
    }

    @Override
    public long countUnreadByUserId(UUID userId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM system_messages WHERE user_id = ? AND is_read = FALSE",
                Long.class,
                userId.toString()
        );
        return count == null ? 0 : count;
    }

    @Override
    public SystemMessage save(SystemMessage message) {
        jdbc.update(
                "MERGE INTO system_messages (id, user_id, message_type, title, content, is_read, "
                        + "payload_json, created_at, read_at) KEY (id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                message.id().toString(),
                message.userId().toString(),
                message.type().value(),
                message.title(),
                message.content(),
                message.read(),
                jsonCodec.write(message.payload()),
                timestamp(message.createdAt()),
                timestamp(message.readAt())
        );
        return message;
    }

    private SystemMessage map(java.sql.ResultSet resultSet, int rowNum) throws java.sql.SQLException {
        String type = resultSet.getString("message_type");
        return new SystemMessage(
                uuid(resultSet.getString("id")),
                uuid(resultSet.getString("user_id")),
                SystemMessageType.valueOf(type.toUpperCase(java.util.Locale.ROOT)),
                resultSet.getString("title"),
                resultSet.getString("content"),
                resultSet.getBoolean("is_read"),
                jsonCodec.read(resultSet.getString("payload_json"), PAYLOAD_TYPE),
                instant(resultSet.getTimestamp("created_at")),
                instant(resultSet.getTimestamp("read_at"))
        );
    }
}
