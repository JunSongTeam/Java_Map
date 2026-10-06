package com.example.mapjava.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record VerifyVerificationCodeRequest(
        @NotBlank(message = "target is required")
        @Size(max = 120, message = "target must be at most 120 characters")
        String target,

        @NotBlank(message = "code is required")
        @Pattern(regexp = "^\\d{6}$", message = "code must be 6 digits")
        String code,

        @Pattern(regexp = "(?i)^(sms|email)$", message = "channel must be sms or email")
        String channel
) {
}
