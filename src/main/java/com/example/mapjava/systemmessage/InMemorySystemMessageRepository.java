package com.example.mapjava.systemmessage;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemorySystemMessageRepository implements SystemMessageRepository {

    private final ConcurrentMap<UUID, SystemMessage> messagesById = new ConcurrentHashMap<>();

    @Override
    public Optional<SystemMessage> findById(UUID id) {
        return Optional.ofNullable(messagesById.get(id));
    }

    @Override
    public List<SystemMessage> findByUserId(UUID userId) {
        return messagesById.values()
                .stream()
                .filter(message -> message.userId().equals(userId))
                .sorted(Comparator.comparing(SystemMessage::createdAt).reversed())
                .toList();
    }

    @Override
    public boolean existsByUserIdAndPayloadValue(UUID userId, String payloadKey, String payloadValue) {
        return messagesById.values()
                .stream()
                .filter(message -> message.userId().equals(userId))
                .anyMatch(message -> payloadValue.equals(message.payload().get(payloadKey)));
    }

    @Override
    public long countUnreadByUserId(UUID userId) {
        return messagesById.values()
                .stream()
                .filter(message -> message.userId().equals(userId))
                .filter(message -> !message.read())
                .count();
    }

    @Override
    public SystemMessage save(SystemMessage message) {
        messagesById.put(message.id(), message);
        return message;
    }
}
