package com.example.mapjava.systemmessage;

import java.util.List;
import java.util.UUID;

import com.example.mapjava.auth.AuthService;

import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system-messages")
public class SystemMessageController {

    private final AuthService authService;
    private final SystemMessageService systemMessageService;

    public SystemMessageController(AuthService authService, SystemMessageService systemMessageService) {
        this.authService = authService;
        this.systemMessageService = systemMessageService;
    }

    @GetMapping
    public List<SystemMessageResponse> messages(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        UUID userId = authService.currentUserId(authorization);
        return systemMessageService.findMessages(userId);
    }

    @GetMapping("/unread-count")
    public SystemMessageUnreadCountResponse unreadCount(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        UUID userId = authService.currentUserId(authorization);
        return systemMessageService.unreadCount(userId);
    }

    @PutMapping("/{messageId}/read")
    public SystemMessageResponse markRead(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @PathVariable UUID messageId
    ) {
        UUID userId = authService.currentUserId(authorization);
        return systemMessageService.markRead(userId, messageId);
    }

    @PutMapping("/read-all")
    public SystemMessageUnreadCountResponse markAllRead(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization
    ) {
        UUID userId = authService.currentUserId(authorization);
        return systemMessageService.markAllRead(userId);
    }
}
