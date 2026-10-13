package com.example.mapjava.membership;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryMembershipOrderRepository implements MembershipOrderRepository {

    private final ConcurrentMap<UUID, MembershipOrder> ordersById = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> orderIdsByTransactionId = new ConcurrentHashMap<>();

    @Override
    public MembershipOrder save(MembershipOrder order) {
        ordersById.put(order.orderId(), order);
        if (order.transactionId() != null && !order.transactionId().isBlank()) {
            orderIdsByTransactionId.put(order.transactionId(), order.orderId());
        }
        return order;
    }

    @Override
    public Optional<MembershipOrder> findById(UUID orderId) {
        return Optional.ofNullable(ordersById.get(orderId));
    }

    @Override
    public Optional<MembershipOrder> findByTransactionId(String transactionId) {
        UUID orderId = orderIdsByTransactionId.get(transactionId);
        if (orderId == null) {
            return Optional.empty();
        }
        return findById(orderId);
    }
}
