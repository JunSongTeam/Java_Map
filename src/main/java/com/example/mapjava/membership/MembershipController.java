package com.example.mapjava.membership;

import java.util.List;
import java.util.UUID;

import com.example.mapjava.auth.AuthService;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/membership")
public class MembershipController {

    private final AuthService authService;
    private final MembershipService membershipService;

    public MembershipController(AuthService authService, MembershipService membershipService) {
        this.authService = authService;
        this.membershipService = membershipService;
    }

    @GetMapping("/products")
    public List<MembershipProductResponse> products() {
        return membershipService.products();
    }

    @GetMapping("/status")
    public MembershipStatusResponse status(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        UUID userId = authService.currentUserId(authorization);
        return membershipService.status(userId);
    }

    @PostMapping("/orders")
    public MembershipOrderResponse createOrder(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody CreateMembershipOrderRequest request
    ) {
        UUID userId = authService.currentUserId(authorization);
        return membershipService.createOrder(userId, request);
    }

    @PostMapping("/orders/verify")
    public MembershipOrderResponse verifyOrder(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody VerifyMembershipOrderRequest request
    ) {
        UUID userId = authService.currentUserId(authorization);
        return membershipService.verifyOrder(userId, request);
    }
}
