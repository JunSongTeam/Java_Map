package com.example.mapjava.auth;

public record AvatarImage(
        byte[] bytes,
        String contentType
) {
}
