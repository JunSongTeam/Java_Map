package com.example.mapjava.auth;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTokenRepository implements TokenRepository {

    private final ConcurrentMap<String, AuthSession> sessions = new ConcurrentHashMap<>();

    @Override
    public Optional<AuthSession> findByToken(String token) {
        return Optional.ofNullable(sessions.get(token));
    }

    @Override
    public AuthSession save(AuthSession session) {
        sessions.put(session.token(), session);
        return session;
    }

    @Override
    public void deleteByToken(String token) {
        sessions.remove(token);
    }
}
