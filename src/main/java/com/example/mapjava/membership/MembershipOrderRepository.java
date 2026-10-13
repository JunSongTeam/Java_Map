package com.example.mapjava.membership;

import java.util.Optional;
import java.util.UUID;

public interface MembershipOrderRepository {

    MembershipOrder save(MembershipOrder order);

    Optional<MembershipOrder> findById(UUID orderId);

    Optional<MembershipOrder> findByTransactionId(String transactionId);
}
