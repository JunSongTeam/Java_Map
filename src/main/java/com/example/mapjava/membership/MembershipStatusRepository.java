package com.example.mapjava.membership;

import java.util.Optional;
import java.util.UUID;

public interface MembershipStatusRepository {

    Optional<MembershipStatus> findByUserId(UUID userId);

    MembershipStatus save(MembershipStatus status);
}
