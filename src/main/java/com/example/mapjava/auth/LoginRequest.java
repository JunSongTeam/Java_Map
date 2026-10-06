package com.example.mapjava.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "phone is required")
        @Size(max = 20, message = "phone must be at most 20 characters")
        String phone,

        @NotBlank(message = "code is required")
        @Pattern(regexp = "^\\d{6}$", message = "code must be 6 digits")
        String code
) {
}
