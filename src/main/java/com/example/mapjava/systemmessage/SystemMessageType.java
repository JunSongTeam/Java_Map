package com.example.mapjava.systemmessage;

public enum SystemMessageType {
    FRIEND_REQUEST("friend_request"),
    FRIEND_ACCEPTED("friend_accepted"),
    SYSTEM_ANNOUNCEMENT("system_announcement");

    private final String value;

    SystemMessageType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
