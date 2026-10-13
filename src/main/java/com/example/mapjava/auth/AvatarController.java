package com.example.mapjava.auth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class AvatarController {

    private final AuthService authService;
    private final AvatarStorageService avatarStorageService;

    public AvatarController(AuthService authService, AvatarStorageService avatarStorageService) {
        this.authService = authService;
        this.avatarStorageService = avatarStorageService;
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfile uploadAvatar(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestParam("file") MultipartFile file
    ) {
        UUID userId = authService.currentUserId(authorization);
        avatarStorageService.save(userId, file);
        return authService.updateCurrentUserAvatar(authorization, avatarUrl(userId));
    }

    @GetMapping("/users/{id}/avatar")
    public ResponseEntity<byte[]> avatar(@PathVariable UUID id) {
        return avatarStorageService.find(id)
                .map(image -> ResponseEntity
                        .ok()
                        .contentType(MediaType.valueOf(image.contentType()))
                        .body(image.bytes()))
                .orElseGet(() -> defaultAvatar(id));
    }

    private ResponseEntity<byte[]> defaultAvatar(UUID id) {
        String label = id.toString().substring(0, 2).toUpperCase();
        String color = "#" + id.toString().replace("-", "").substring(0, 6);
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="128" height="128" viewBox="0 0 128 128">
                  <rect width="128" height="128" rx="64" fill="%s"/>
                  <text x="64" y="74" text-anchor="middle" font-family="Arial, sans-serif" font-size="34" font-weight="700" fill="#fff">%s</text>
                </svg>
                """.formatted(color, label);

        return ResponseEntity
                .ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .body(svg.getBytes(StandardCharsets.UTF_8));
    }

    private static String avatarUrl(UUID userId) {
        return "/api/users/" + userId + "/avatar";
    }
}
