package com.example.mapjava.track;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrackRepository {

    Optional<Track> findById(UUID id);

    Optional<Track> findByUserIdAndDate(UUID userId, LocalDate date);

    List<Track> findByUserId(UUID userId);

    Track save(Track track);
}
