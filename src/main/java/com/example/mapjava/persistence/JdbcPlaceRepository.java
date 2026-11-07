package com.example.mapjava.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.mapjava.place.Place;
import com.example.mapjava.place.PlaceRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db")
public class JdbcPlaceRepository extends JdbcRepositorySupport implements PlaceRepository {

    public JdbcPlaceRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public List<Place> findAll() {
        return jdbc.query(
                "SELECT id, name, category, latitude, longitude, description, created_at, updated_at "
                        + "FROM places ORDER BY created_at DESC",
                this::map
        );
    }

    @Override
    public Optional<Place> findById(UUID id) {
        return jdbc.query(
                "SELECT id, name, category, latitude, longitude, description, created_at, updated_at "
                        + "FROM places WHERE id = ?",
                this::map,
                id.toString()
        ).stream().findFirst();
    }

    @Override
    public Place save(Place place) {
        jdbc.update(
                "MERGE INTO places (id, name, category, latitude, longitude, description, created_at, updated_at) "
                        + "KEY (id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                place.id().toString(),
                place.name(),
                place.category(),
                place.latitude(),
                place.longitude(),
                place.description(),
                timestamp(place.createdAt()),
                timestamp(place.updatedAt())
        );
        return place;
    }

    @Override
    public void deleteById(UUID id) {
        jdbc.update("DELETE FROM places WHERE id = ?", id.toString());
    }

    @Override
    public boolean existsById(UUID id) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM places WHERE id = ?",
                Integer.class,
                id.toString()
        );
        return count != null && count > 0;
    }

    private Place map(java.sql.ResultSet resultSet, int rowNum) throws java.sql.SQLException {
        return new Place(
                uuid(resultSet.getString("id")),
                resultSet.getString("name"),
                resultSet.getString("category"),
                resultSet.getDouble("latitude"),
                resultSet.getDouble("longitude"),
                resultSet.getString("description"),
                instant(resultSet.getTimestamp("created_at")),
                instant(resultSet.getTimestamp("updated_at"))
        );
    }
}
