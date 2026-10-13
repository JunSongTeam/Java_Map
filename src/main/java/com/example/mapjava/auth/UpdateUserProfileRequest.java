package com.example.mapjava.auth;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @Size(max = 40, message = "昵称不能超过 40 个字符")
        String nickname,

        @Size(max = 500, message = "头像地址不能超过 500 个字符")
        String avatarUrl
) {
}
