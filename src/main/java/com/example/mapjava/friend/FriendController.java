package com.example.mapjava.friend;

import java.util.List;
import java.util.UUID;

import com.example.mapjava.auth.AuthService;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class FriendController {

    private final AuthService authService;
    private final FriendService friendService;

    public FriendController(AuthService authService, FriendService friendService) {
        this.authService = authService;
        this.friendService = friendService;
    }

    @GetMapping("/users/registered")
    public RegisteredUserResponse registeredUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(required = false) String phone
    ) {
        authService.currentUserId(authorization);
        return friendService.findRegisteredUser(phone);
    }

    @PostMapping("/friends")
    public FriendRequestResponse addFriend(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @Valid @RequestBody AddFriendRequest request
    ) {
        UUID userId = authService.currentUserId(authorization);
        return friendService.sendFriendRequest(userId, request);
    }

    @GetMapping("/friends")
    public List<FriendResponse> friends(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        UUID userId = authService.currentUserId(authorization);
        return friendService.findFriends(userId);
    }

    @PutMapping("/friends/{friendUserId}/remark")
    public FriendResponse updateFriendRemark(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID friendUserId,
            @Valid @RequestBody UpdateFriendRemarkRequest request
    ) {
        UUID userId = authService.currentUserId(authorization);
        return friendService.updateFriendRemark(userId, friendUserId, request);
    }

    @DeleteMapping("/friends/{friendUserId}")
    public ResponseEntity<Void> deleteFriend(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID friendUserId
    ) {
        UUID userId = authService.currentUserId(authorization);
        friendService.deleteFriend(userId, friendUserId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/friends/requests")
    public List<FriendRequestResponse> incomingFriendRequests(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        UUID userId = authService.currentUserId(authorization);
        return friendService.findIncomingRequests(userId);
    }

    @PostMapping("/friends/requests/{requestId}/accept")
    public FriendResponse acceptFriendRequest(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID requestId
    ) {
        UUID userId = authService.currentUserId(authorization);
        return friendService.acceptFriendRequest(userId, requestId);
    }
}
