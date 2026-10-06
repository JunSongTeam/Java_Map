package com.example.mapjava.auth;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUserRepository implements UserRepository {

    private final ConcurrentMap<UUID, AppUser> usersById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> userIdsByPhone = new ConcurrentHashMap<>();

    @Override
    public Optional<AppUser> findById(UUID id) {
        return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public Optional<AppUser> findByPhone(String phone) {
        UUID id = userIdsByPhone.get(phone);
        if (id == null) {
            return Optional.empty();
        }

        return findById(id);
    }

    @Override
    public AppUser save(AppUser user) {
        usersById.put(user.id(), user);
        userIdsByPhone.put(user.phone(), user.id());
        return user;
    }
}
