package com.example.mapjava.auth;

import java.util.Optional;

public interface VerificationCodeRepository {

    Optional<VerificationCode> findByChannelAndTarget(String channel, String target);

    VerificationCode save(VerificationCode verificationCode);

    void deleteByChannelAndTarget(String channel, String target);
}
