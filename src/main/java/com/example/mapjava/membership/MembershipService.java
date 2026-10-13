package com.example.mapjava.membership;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import com.example.mapjava.common.BadRequestException;
import org.springframework.stereotype.Service;

@Service
public class MembershipService {

    private static final String ACTIVE = "active";
    private static final String INACTIVE = "inactive";
    private static final String DEFAULT_PAYMENT_PROVIDER = "apple_iap";
    private static final Duration ORDER_TTL = Duration.ofMinutes(30);
    private static final List<MembershipProduct> PRODUCTS = List.of(
            new MembershipProduct(
                    "20231004",
                    "永久会员",
                    "开通后可永久使用会员功能",
                    36500,
                    16800,
                    "CNY",
                    "location.vip.permanent",
                    "永久会员",
                    "¥168",
                    "¥688",
                    "¥0.01/天",
                    "20231004",
                    "1"
            ),
            new MembershipProduct(
                    "20231003",
                    "季度会员",
                    "开通后可使用会员功能 90 天",
                    90,
                    5800,
                    "CNY",
                    "location.vip.quarterly",
                    "季度会员",
                    "¥58",
                    "¥88",
                    "¥0.7/天",
                    "20231003",
                    "0"
            ),
            new MembershipProduct(
                    "20231002",
                    "月会员",
                    "开通后可使用会员功能 31 天",
                    31,
                    3800,
                    "CNY",
                    "location.vip.monthly",
                    "月会员",
                    "¥38",
                    "¥68",
                    "¥1.2/天",
                    "20231002",
                    "0"
            ),
            new MembershipProduct(
                    "20231001",
                    "周会员",
                    "开通后可使用会员功能 7 天",
                    7,
                    2800,
                    "CNY",
                    "location.vip.weekly",
                    "周会员",
                    "¥28",
                    "¥38",
                    "¥4/天",
                    "20231001",
                    "0"
            )
    );

    private final MembershipOrderRepository orderRepository;
    private final MembershipStatusRepository statusRepository;
    private final Clock clock;

    public MembershipService(
            MembershipOrderRepository orderRepository,
            MembershipStatusRepository statusRepository,
            Clock clock
    ) {
        this.orderRepository = orderRepository;
        this.statusRepository = statusRepository;
        this.clock = clock;
    }

    public List<MembershipProductResponse> products() {
        return PRODUCTS.stream()
                .map(this::toProductResponse)
                .toList();
    }

    public MembershipStatusResponse status(UUID userId) {
        return statusRepository.findByUserId(userId)
                .map(this::toStatusResponse)
                .orElseGet(() -> new MembershipStatusResponse(INACTIVE, null, false));
    }

    public MembershipOrderResponse createOrder(UUID userId, CreateMembershipOrderRequest request) {
        MembershipProduct product = findProduct(request.productId());
        Instant now = Instant.now(clock);
        MembershipOrder order = new MembershipOrder(
                UUID.randomUUID(),
                userId,
                product.productId(),
                normalizePaymentProvider(request.paymentProvider()),
                MembershipOrderStatus.PENDING,
                product.priceCents(),
                product.currency(),
                now,
                now.plus(ORDER_TTL),
                null,
                null
        );

        return toOrderResponse(orderRepository.save(order));
    }

    public MembershipOrderResponse verifyOrder(UUID userId, VerifyMembershipOrderRequest request) {
        MembershipOrder order = orderRepository.findById(request.orderId())
                .filter(found -> found.userId().equals(userId))
                .orElseThrow(() -> new BadRequestException("订单不存在"));

        if (order.status() == MembershipOrderStatus.PAID) {
            return toOrderResponse(order);
        }

        Instant now = Instant.now(clock);
        if (order.expiresAt().isBefore(now)) {
            throw new BadRequestException("订单已过期，请重新创建订单");
        }

        String transactionId = normalizePaymentCredential(request.transactionId(), request.receiptData());
        orderRepository.findByTransactionId(transactionId)
                .filter(existing -> !existing.orderId().equals(order.orderId()))
                .ifPresent(existing -> {
                    throw new BadRequestException("该支付凭证已被使用");
                });

        MembershipProduct product = findProduct(order.productId());
        Instant memberExpiresAt = extendMembership(userId, product.durationDays(), now);
        MembershipOrder paidOrder = new MembershipOrder(
                order.orderId(),
                order.userId(),
                order.productId(),
                order.paymentProvider(),
                MembershipOrderStatus.PAID,
                order.priceCents(),
                order.currency(),
                order.createdAt(),
                order.expiresAt(),
                transactionId,
                now
        );

        orderRepository.save(paidOrder);
        return toOrderResponse(paidOrder, new MembershipStatusResponse(ACTIVE, memberExpiresAt, true));
    }

    private Instant extendMembership(UUID userId, int durationDays, Instant now) {
        Instant currentExpiresAt = statusRepository.findByUserId(userId)
                .map(MembershipStatus::expiresAt)
                .filter(expiresAt -> expiresAt.isAfter(now))
                .orElse(now);
        Instant nextExpiresAt = currentExpiresAt.plus(durationDays, ChronoUnit.DAYS);
        statusRepository.save(new MembershipStatus(userId, nextExpiresAt, now));
        return nextExpiresAt;
    }

    private MembershipOrderResponse toOrderResponse(MembershipOrder order) {
        return toOrderResponse(order, status(order.userId()));
    }

    private MembershipOrderResponse toOrderResponse(MembershipOrder order, MembershipStatusResponse status) {
        MembershipProduct product = findProduct(order.productId());
        return new MembershipOrderResponse(
                order.orderId(),
                order.productId(),
                product.title(),
                order.paymentProvider(),
                order.status().name().toLowerCase(),
                order.priceCents(),
                order.currency(),
                order.createdAt(),
                order.expiresAt(),
                order.transactionId(),
                order.paidAt(),
                status.memberStatus(),
                status.memberExpiresAt()
        );
    }

    private MembershipStatusResponse toStatusResponse(MembershipStatus status) {
        boolean active = status.expiresAt().isAfter(Instant.now(clock));
        return new MembershipStatusResponse(
                active ? ACTIVE : INACTIVE,
                active ? status.expiresAt() : null,
                active
        );
    }

    private MembershipProductResponse toProductResponse(MembershipProduct product) {
        return new MembershipProductResponse(
                product.productId(),
                product.title(),
                product.description(),
                product.durationDays(),
                product.priceCents(),
                product.currency(),
                product.appleProductId(),
                product.time(),
                product.price(),
                product.originalPrice(),
                product.unitPrice(),
                product.orderID(),
                product.status()
        );
    }

    private static MembershipProduct findProduct(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new BadRequestException("会员商品不存在");
        }

        return PRODUCTS.stream()
                .filter(product -> product.productId().equals(productId.trim()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("会员商品不存在"));
    }

    private static String normalizePaymentProvider(String paymentProvider) {
        if (paymentProvider == null || paymentProvider.isBlank()) {
            return DEFAULT_PAYMENT_PROVIDER;
        }
        return paymentProvider.trim();
    }

    private static String normalizePaymentCredential(String transactionId, String receiptData) {
        if (transactionId != null && !transactionId.isBlank()) {
            return transactionId.trim();
        }

        if (receiptData != null && !receiptData.isBlank()) {
            return receiptData.trim();
        }

        throw new BadRequestException("支付凭证不能为空");
    }
}
