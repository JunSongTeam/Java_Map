package com.example.mapjava.friend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddFriendRequest(
        @NotBlank(message = "手机号不能为空")
        @Size(max = 20, message = "手机号不能超过 20 个字符")
        String phone
) {
}
