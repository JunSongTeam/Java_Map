package com.example.mapjava.friend;

import jakarta.validation.constraints.Size;

public record UpdateFriendRemarkRequest(
        @Size(max = 40, message = "备注不能超过 40 个字符")
        String remark
) {
}
