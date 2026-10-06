package com.example.mapjava.track;

import java.util.List;

public record TrackUploadResponse(
        int uploadedCount,
        List<AffectedTrackResponse> affectedTracks
) {
}
