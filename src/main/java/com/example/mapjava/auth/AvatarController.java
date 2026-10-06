package com.example.mapjava.auth;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class AvatarController {

    @GetMapping(value = "/{id}/avatar", produces = "image/svg+xml")
    public ResponseEntity<String> defaultAvatar(@PathVariable UUID id) {
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
                .body(svg);
    }
}
