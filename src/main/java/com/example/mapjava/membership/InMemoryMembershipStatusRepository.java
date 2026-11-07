package com.example.mapjava.membership;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Profile;

@Repository
@Profile("!db")
public class InMemoryMembershipStatusRepository implements MembershipStatusRepository {

    private final ConcurrentMap<UUID, MembershipStatus> statusesByUserId = new ConcurrentHashMap<>();

    @Override
    public Optional<MembershipStatus> findByUserId(UUID userId) {
        return Optional.ofNullable(statusesByUserId.get(userId));
    }

    @Override
    public MembershipStatus save(MembershipStatus status) {
        statusesByUserId.put(status.userId(), status);
        return status;
    }
}
