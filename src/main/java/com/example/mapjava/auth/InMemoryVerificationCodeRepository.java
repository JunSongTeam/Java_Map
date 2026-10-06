package com.example.mapjava.auth;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryVerificationCodeRepository implements VerificationCodeRepository {

    private final ConcurrentMap<String, VerificationCode> codes = new ConcurrentHashMap<>();

    @Override
    public Optional<VerificationCode> findByChannelAndTarget(String channel, String target) {
        return Optional.ofNullable(codes.get(key(channel, target)));
    }

    @Override
    public VerificationCode save(VerificationCode verificationCode) {
        codes.put(key(verificationCode.channel(), verificationCode.target()), verificationCode);
        return verificationCode;
    }

    @Override
    public void deleteByChannelAndTarget(String channel, String target) {
        codes.remove(key(channel, target));
    }

    private static String key(String channel, String target) {
        return channel + ":" + target;
    }
}
