package com.example.mapjava.track;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.example.mapjava.common.BadRequestException;
import com.example.mapjava.friend.FriendRepository;
import org.springframework.stereotype.Service;

@Service
public class TrackService {

    private static final ZoneId TRACK_DATE_ZONE = ZoneId.of("Asia/Shanghai");
    private static final double EARTH_RADIUS_METERS = 6_371_000;

    private final TrackRepository trackRepository;
    private final FriendRepository friendRepository;
    private final Clock clock;

    public TrackService(TrackRepository trackRepository, FriendRepository friendRepository, Clock clock) {
        this.trackRepository = trackRepository;
        this.friendRepository = friendRepository;
        this.clock = clock;
    }

    public TrackUploadResponse upload(UUID userId, List<TrackPointRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("Track points are required");
        }

        Map<LocalDate, List<TrackPoint>> pointsByDate = new LinkedHashMap<>();
        for (TrackPointRequest request : requests) {
            TrackPoint point = toPoint(request);
            LocalDate date = LocalDate.ofInstant(point.recordedAt(), TRACK_DATE_ZONE);
            pointsByDate.computeIfAbsent(date, ignored -> new ArrayList<>()).add(point);
        }

        List<AffectedTrackResponse> affectedTracks = new ArrayList<>();
        Instant now = Instant.now(clock);
        int uploadedCount = 0;

        for (Map.Entry<LocalDate, List<TrackPoint>> entry : pointsByDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<TrackPoint> incomingPoints = entry.getValue();
            Track existing = trackRepository.findByUserIdAndDate(userId, date)
                    .orElseGet(() -> new Track(UUID.randomUUID(), userId, date, List.of(), now, now));

            List<TrackPoint> mergedPoints = mergePoints(existing.points(), incomingPoints);
            Track saved = trackRepository.save(new Track(
                    existing.id(),
                    existing.userId(),
                    existing.date(),
                    mergedPoints,
                    existing.createdAt(),
                    now
            ));

            uploadedCount += incomingPoints.size();
            affectedTracks.add(new AffectedTrackResponse(saved.id(), saved.date()));
        }

        return new TrackUploadResponse(uploadedCount, affectedTracks);
    }

    public List<TrackSummaryResponse> findMyTracks(UUID userId) {
        return trackRepository.findByUserId(userId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public List<TrackSummaryResponse> findFriendTracks(UUID userId, UUID friendUserId) {
        friendRepository.findBetween(userId, friendUserId)
                .orElseThrow(() -> new BadRequestException("好友不存在"));

        return trackRepository.findByUserId(friendUserId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    public TrackDetailResponse getTrackDetail(UUID userId, UUID trackId) {
        Track track = trackRepository.findById(trackId)
                .filter(found -> found.userId().equals(userId))
                .orElseThrow(() -> new TrackNotFoundException(trackId));

        List<TrackPoint> points = sortedPoints(track.points());
        return new TrackDetailResponse(
                track.id(),
                track.date(),
                startAddress(points),
                endAddress(points),
                startedAt(points),
                endedAt(points),
                distanceMeters(points),
                points.size(),
                points.stream().map(this::toPointResponse).toList()
        );
    }

    private TrackSummaryResponse toSummary(Track track) {
        List<TrackPoint> points = sortedPoints(track.points());
        return new TrackSummaryResponse(
                track.id(),
                track.date(),
                startAddress(points),
                endAddress(points)
        );
    }

    private TrackPoint toPoint(TrackPointRequest request) {
        return new TrackPoint(
                request.latitude(),
                request.longitude(),
                request.recordedAt(),
                normalizeAddress(request.address()),
                request.accuracy(),
                request.speed()
        );
    }

    private TrackPointResponse toPointResponse(TrackPoint point) {
        return new TrackPointResponse(
                point.latitude(),
                point.longitude(),
                point.recordedAt(),
                point.address(),
                point.accuracy(),
                point.speed()
        );
    }

    private static List<TrackPoint> mergePoints(List<TrackPoint> existing, List<TrackPoint> incoming) {
        Map<String, TrackPoint> pointsByKey = new LinkedHashMap<>();
        for (TrackPoint point : existing) {
            pointsByKey.put(pointKey(point), point);
        }
        for (TrackPoint point : incoming) {
            pointsByKey.putIfAbsent(pointKey(point), point);
        }

        return pointsByKey.values()
                .stream()
                .sorted(Comparator.comparing(TrackPoint::recordedAt))
                .toList();
    }

    private static List<TrackPoint> sortedPoints(List<TrackPoint> points) {
        return points.stream()
                .sorted(Comparator.comparing(TrackPoint::recordedAt))
                .toList();
    }

    private static String startAddress(List<TrackPoint> points) {
        return points.isEmpty() ? "" : addressOrCoordinate(points.get(0));
    }

    private static String endAddress(List<TrackPoint> points) {
        return points.isEmpty() ? "" : addressOrCoordinate(points.get(points.size() - 1));
    }

    private static Instant startedAt(List<TrackPoint> points) {
        return points.isEmpty() ? null : points.get(0).recordedAt();
    }

    private static Instant endedAt(List<TrackPoint> points) {
        return points.isEmpty() ? null : points.get(points.size() - 1).recordedAt();
    }

    private static double distanceMeters(List<TrackPoint> points) {
        double total = 0;
        for (int i = 1; i < points.size(); i++) {
            total += distanceBetween(points.get(i - 1), points.get(i));
        }

        return Math.round(total * 10.0) / 10.0;
    }

    private static double distanceBetween(TrackPoint first, TrackPoint second) {
        double lat1 = Math.toRadians(first.latitude());
        double lat2 = Math.toRadians(second.latitude());
        double deltaLat = Math.toRadians(second.latitude() - first.latitude());
        double deltaLng = Math.toRadians(second.longitude() - first.longitude());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private static String addressOrCoordinate(TrackPoint point) {
        if (point.address() != null && !point.address().isBlank()) {
            return point.address();
        }

        return "%.6f,%.6f".formatted(point.latitude(), point.longitude());
    }

    private static String normalizeAddress(String address) {
        if (address == null || address.isBlank()) {
            return "";
        }

        return address.trim();
    }

    private static String pointKey(TrackPoint point) {
        return point.recordedAt() + ":" + point.latitude() + ":" + point.longitude();
    }
}
