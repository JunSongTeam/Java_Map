package com.example.mapjava.persistence;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

abstract class JdbcRepositorySupport {

    protected final JdbcTemplate jdbc;

    protected JdbcRepositorySupport(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    protected static Timestamp timestamp(Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    protected static Instant instant(Timestamp value) {
        return value == null ? null : value.toInstant();
    }

    protected static UUID uuid(String value) {
        return UUID.fromString(value);
    }
}
