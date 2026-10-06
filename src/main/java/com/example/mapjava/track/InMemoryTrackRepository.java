package com.example.mapjava.track;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTrackRepository implements TrackRepository {

    private final ConcurrentMap<UUID, Track> tracksById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> trackIdsByUserAndDate = new ConcurrentHashMap<>();

    @Override
    public Optional<Track> findById(UUID id) {
        return Optional.ofNullable(tracksById.get(id));
    }

    @Override
    public Optional<Track> findByUserIdAndDate(UUID userId, LocalDate date) {
        UUID id = trackIdsByUserAndDate.get(key(userId, date));
        if (id == null) {
            return Optional.empty();
        }

        return findById(id);
    }

    @Override
    public List<Track> findByUserId(UUID userId) {
        return tracksById.values()
                .stream()
                .filter(track -> track.userId().equals(userId))
                .sorted(Comparator.comparing(Track::date).reversed())
                .toList();
    }

    @Override
    public Track save(Track track) {
        tracksById.put(track.id(), track);
        trackIdsByUserAndDate.put(key(track.userId(), track.date()), track.id());
        return track;
    }

    private static String key(UUID userId, LocalDate date) {
        return userId + ":" + date;
    }
}
