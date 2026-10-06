package com.example.mapjava.auth;

import java.util.Optional;

public interface TokenRepository {

    Optional<AuthSession> findByToken(String token);

    AuthSession save(AuthSession session);

    void deleteByToken(String token);
}
