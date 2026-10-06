package com.example.mapjava.auth;

public interface VerificationCodeSender {

    void send(String channel, String target, String code);
}
