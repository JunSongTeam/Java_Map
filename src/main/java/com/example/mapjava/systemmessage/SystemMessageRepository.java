package com.example.mapjava.systemmessage;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SystemMessageRepository {

    Optional<SystemMessage> findById(UUID id);

    List<SystemMessage> findByUserId(UUID userId);

    boolean existsByUserIdAndPayloadValue(UUID userId, String payloadKey, String payloadValue);

    long countUnreadByUserId(UUID userId);

    SystemMessage save(SystemMessage message);
}
