package com.example.mapjava.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.mapjava.track.Track;
import com.example.mapjava.track.TrackPoint;
import com.example.mapjava.track.TrackRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db")
public class JdbcTrackRepository extends JdbcRepositorySupport implements TrackRepository {

    private static final TypeReference<List<TrackPoint>> POINTS_TYPE = new TypeReference<>() {};

    private final JdbcJsonCodec jsonCodec;

    public JdbcTrackRepository(JdbcTemplate jdbc, JdbcJsonCodec jsonCodec) {
        super(jdbc);
        this.jsonCodec = jsonCodec;
    }

    @Override
    public Optional<Track> findById(UUID id) {
        return jdbc.query(
                "SELECT id, user_id, track_date, points_json, created_at, updated_at FROM tracks WHERE id = ?",
                this::map,
                id.toString()
        ).stream().findFirst();
    }

    @Override
    public Optional<Track> findByUserIdAndDate(UUID userId, LocalDate date) {
        return jdbc.query(
                "SELECT id, user_id, track_date, points_json, created_at, updated_at FROM tracks "
                        + "WHERE user_id = ? AND track_date = ?",
                this::map,
                userId.toString(),
                java.sql.Date.valueOf(date)
        ).stream().findFirst();
    }

    @Override
    public List<Track> findByUserId(UUID userId) {
        return jdbc.query(
                "SELECT id, user_id, track_date, points_json, created_at, updated_at FROM tracks "
                        + "WHERE user_id = ? ORDER BY track_date DESC",
                this::map,
                userId.toString()
        );
    }

    @Override
    public Track save(Track track) {
        jdbc.update(
                "MERGE INTO tracks (id, user_id, track_date, points_json, created_at, updated_at) "
                        + "KEY (user_id, track_date) VALUES (?, ?, ?, ?, ?, ?)",
                track.id().toString(),
                track.userId().toString(),
                java.sql.Date.valueOf(track.date()),
                jsonCodec.write(track.points()),
                timestamp(track.createdAt()),
                timestamp(track.updatedAt())
        );
        return track;
    }

    private Track map(java.sql.ResultSet resultSet, int rowNum) throws java.sql.SQLException {
        return new Track(
                uuid(resultSet.getString("id")),
                uuid(resultSet.getString("user_id")),
                resultSet.getDate("track_date").toLocalDate(),
                jsonCodec.read(resultSet.getString("points_json"), POINTS_TYPE),
                instant(resultSet.getTimestamp("created_at")),
                instant(resultSet.getTimestamp("updated_at"))
        );
    }
}
