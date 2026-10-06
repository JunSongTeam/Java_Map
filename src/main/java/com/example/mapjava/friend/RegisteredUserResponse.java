package com.example.mapjava.friend;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RegisteredUserResponse(
        String phone,
        boolean registered,
        String message,
        UserLookupProfile user
) {
}
