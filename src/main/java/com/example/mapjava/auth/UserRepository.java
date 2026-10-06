package com.example.mapjava.auth;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<AppUser> findById(UUID id);

    Optional<AppUser> findByPhone(String phone);

    AppUser save(AppUser user);
}
