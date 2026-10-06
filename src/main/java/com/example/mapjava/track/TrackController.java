package com.example.mapjava.track;

import java.util.List;
import java.util.UUID;

import com.example.mapjava.auth.AuthService;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TrackController {

    private final AuthService authService;
    private final TrackService trackService;

    public TrackController(AuthService authService, TrackService trackService) {
        this.authService = authService;
        this.trackService = trackService;
    }

    @PostMapping("/tracks/upload")
    public TrackUploadResponse uploadTracks(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody List<@Valid TrackPointRequest> points
    ) {
        UUID userId = authService.currentUserId(authorization);
        return trackService.upload(userId, points);
    }

    @GetMapping("/me/tracks")
    public List<TrackSummaryResponse> myTracks(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        UUID userId = authService.currentUserId(authorization);
        return trackService.findMyTracks(userId);
    }

    @GetMapping("/friends/{friendUserId}/tracks")
    public List<TrackSummaryResponse> friendTracks(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID friendUserId
    ) {
        UUID userId = authService.currentUserId(authorization);
        return trackService.findFriendTracks(userId, friendUserId);
    }

    @GetMapping("/tracks/{trackId}")
    public TrackDetailResponse trackDetail(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID trackId
    ) {
        UUID userId = authService.currentUserId(authorization);
        return trackService.getTrackDetail(userId, trackId);
    }
}
