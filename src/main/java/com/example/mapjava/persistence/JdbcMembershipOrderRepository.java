package com.example.mapjava.persistence;

import java.util.Optional;
import java.util.UUID;

import com.example.mapjava.membership.MembershipOrder;
import com.example.mapjava.membership.MembershipOrderRepository;
import com.example.mapjava.membership.MembershipOrderStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Profile("db")
public class JdbcMembershipOrderRepository extends JdbcRepositorySupport implements MembershipOrderRepository {

    public JdbcMembershipOrderRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    public MembershipOrder save(MembershipOrder order) {
        jdbc.update(
                "MERGE INTO membership_orders (order_id, user_id, product_id, payment_provider, status, "
                        + "price_cents, currency, created_at, expires_at, transaction_id, paid_at) "
                        + "KEY (order_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                order.orderId().toString(),
                order.userId().toString(),
                order.productId(),
                order.paymentProvider(),
                order.status().name(),
                order.priceCents(),
                order.currency(),
                timestamp(order.createdAt()),
                timestamp(order.expiresAt()),
                order.transactionId(),
                timestamp(order.paidAt())
        );
        return order;
    }

    @Override
    public Optional<MembershipOrder> findById(UUID orderId) {
        return jdbc.query(
                "SELECT order_id, user_id, product_id, payment_provider, status, price_cents, currency, "
                        + "created_at, expires_at, transaction_id, paid_at FROM membership_orders WHERE order_id = ?",
                this::map,
                orderId.toString()
        ).stream().findFirst();
    }

    @Override
    public Optional<MembershipOrder> findByTransactionId(String transactionId) {
        return jdbc.query(
                "SELECT order_id, user_id, product_id, payment_provider, status, price_cents, currency, "
                        + "created_at, expires_at, transaction_id, paid_at FROM membership_orders "
                        + "WHERE transaction_id = ?",
                this::map,
                transactionId
        ).stream().findFirst();
    }

    private MembershipOrder map(java.sql.ResultSet resultSet, int rowNum) throws java.sql.SQLException {
        return new MembershipOrder(
                uuid(resultSet.getString("order_id")),
                uuid(resultSet.getString("user_id")),
                resultSet.getString("product_id"),
                resultSet.getString("payment_provider"),
                MembershipOrderStatus.valueOf(resultSet.getString("status")),
                resultSet.getLong("price_cents"),
                resultSet.getString("currency"),
                instant(resultSet.getTimestamp("created_at")),
                instant(resultSet.getTimestamp("expires_at")),
                resultSet.getString("transaction_id"),
                instant(resultSet.getTimestamp("paid_at"))
        );
    }
}
